/*
 * Copyright (c) 2018 Hugo Dupanloup (Yeregorix)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.smoofyuniverse.antixray;

import com.google.inject.Inject;
import net.smoofyuniverse.antixray.api.modifier.ChunkModifier;
import net.smoofyuniverse.antixray.api.modifier.ChunkModifierRegistryModule;
import net.smoofyuniverse.antixray.api.volume.ChunkView.State;
import net.smoofyuniverse.antixray.bstats.MetricsLite;
import net.smoofyuniverse.antixray.config.global.GlobalConfig;
import net.smoofyuniverse.antixray.config.serializer.BlockSetSerializer;
import net.smoofyuniverse.antixray.event.PlayerEventListener;
import net.smoofyuniverse.antixray.event.WorldEventListener;
import net.smoofyuniverse.antixray.impl.internal.InternalWorld;
import net.smoofyuniverse.antixray.impl.network.NetworkChunk;
import net.smoofyuniverse.antixray.ore.OreAPI;
import net.smoofyuniverse.antixray.util.collection.BlockSet;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.GuiceObjectMapperFactory;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static net.smoofyuniverse.antixray.util.MathUtil.clamp;

@Plugin(id = "antixray", name = "AntiXray", version = "1.3.0", authors = "Yeregorix", description = "A powerful solution against xray users")
public class AntiXray {
	public static final int CURRENT_CONFIG_VERSION = 1, MINIMUM_CONFIG_VERSION = 1;

	public static final Logger LOGGER = LoggerFactory.getLogger("AntiXray");
	private static AntiXray instance;

	@Inject
	private Game game;
	@Inject
	@ConfigDir(sharedRoot = false)
	private Path configDir;
	@Inject
	private GuiceObjectMapperFactory factory;
	@Inject
	private PluginContainer container;

	@Inject
	private MetricsLite metrics;

	private ConfigurationOptions configOptions;
	private Task updateTask;
	private Path cacheDir, worldConfigsDir;

	private GlobalConfig.Immutable globalConfig;
	private Text[] updateMessages = new Text[0];

	public AntiXray() {
		if (instance != null)
			throw new IllegalStateException();
		instance = this;
	}

	@Listener
	public void onGameConstruction(GameConstructionEvent e) {
		this.game.getRegistry().registerModule(ChunkModifier.class, ChunkModifierRegistryModule.get());
		TypeSerializers.getDefaultSerializers().registerType(BlockSet.TOKEN, new BlockSetSerializer());
	}

	@Listener
	public void onGamePreInit(GamePreInitializationEvent e) {
		this.cacheDir = this.game.getGameDirectory().resolve("antixray-cache");
		this.worldConfigsDir = this.configDir.resolve("worlds");
		try {
			Files.createDirectories(this.worldConfigsDir);
		} catch (IOException ignored) {
		}
		this.configOptions = ConfigurationOptions.defaults().setObjectMapperFactory(this.factory).setShouldCopyDefaults(true);

		LOGGER.info("Loading global configuration ..");
		try {
			loadGlobalConfig();
		} catch (Exception ex) {
			LOGGER.error("Failed to load global configuration", ex);
		}

		this.game.getEventManager().registerListeners(this, new WorldEventListener());
		this.game.getEventManager().registerListeners(this, new PlayerEventListener());
	}

	public void loadGlobalConfig() throws IOException, ObjectMappingException {
		if (this.globalConfig != null)
			throw new IllegalStateException("Config already loaded");

		Path file = this.configDir.resolve("global.conf");
		ConfigurationLoader<CommentedConfigurationNode> loader = createConfigLoader(file);

		CommentedConfigurationNode root = loader.load();
		int version = root.getNode("Version").getInt();
		if ((version > CURRENT_CONFIG_VERSION || version < MINIMUM_CONFIG_VERSION) && backupFile(file)) {
			LOGGER.info("Your global config version is not supported. A new one will be generated.");
			root = loader.createEmptyNode();
		}

		ConfigurationNode cfgNode = root.getNode("Config");
		GlobalConfig cfg = cfgNode.getValue(GlobalConfig.TOKEN, new GlobalConfig());

		cfg.updateCheck.consoleDelay = clamp(cfg.updateCheck.consoleDelay, -1, 100);
		cfg.updateCheck.playerDelay = clamp(cfg.updateCheck.playerDelay, -1, 100);

		if (cfg.updateCheck.consoleDelay == -1 && cfg.updateCheck.playerDelay == -1)
			cfg.updateCheck.enabled = false;

		version = CURRENT_CONFIG_VERSION;
		root.getNode("Version").setValue(version);
		cfgNode.setValue(GlobalConfig.TOKEN, cfg);
		loader.save(root);

		this.globalConfig = cfg.toImmutable();
	}

	@Listener
	public void onServerStopping(GameStoppingServerEvent e) {
		if (this.updateTask != null) {
			this.updateTask.cancel();
			this.updateTask = null;
		}
	}

	@Listener
	public void onServerStarted(GameStartedServerEvent e) {
		if (this.game.getServer().getWorlds().iterator().next() instanceof InternalWorld) {
			this.updateTask = Task.builder().execute(() -> {
				for (World w : this.game.getServer().getWorlds()) {
					for (NetworkChunk chunk : ((InternalWorld) w).getView().getLoadedChunkViews()) {
						if (chunk.getState() == State.PREOBFUSCATED)
							chunk.obfuscate();
					}
				}
			}).intervalTicks(1).submit(this);

			LOGGER.info("AntiXray " + this.container.getVersion().orElse("?") + " was loaded successfully.");
		} else {
			LOGGER.error("!!WARNING!! AntiXray was not loaded correctly. Be sure that the jar file is at the root of your mods folder!");
		}

		if (this.globalConfig.updateCheck.enabled)
			Task.builder().async().execute(this::checkForUpdate).submit(this);
	}

	public void checkForUpdate() {
		String version = this.container.getVersion().orElse(null);
		if (version == null)
			return;

		String latestVersion = null;
		try {
			latestVersion = OreAPI.getLatestVersion(OreAPI.getProjectVersions("antixray"), "7.1.0").orElse(null);
		} catch (Exception e) {
			LOGGER.info("Failed to check for update", e);
		}

		if (latestVersion != null && !latestVersion.equals(version)) {
			String downloadUrl = "https://ore.spongepowered.org/Yeregorix/AntiXray/versions/" + latestVersion;

			Text msg1 = Text.join(Text.of("A new version of AntiXray is available: "),
					Text.builder(latestVersion).color(TextColors.AQUA).build(),
					Text.of(". You're currently using version: "),
					Text.builder(version).color(TextColors.AQUA).build(),
					Text.of("."));

			Text msg2;
			try {
				msg2 = Text.builder("Click here to open the download page.").color(TextColors.GOLD)
						.onClick(TextActions.openUrl(new URL(downloadUrl))).build();
			} catch (MalformedURLException e) {
				msg2 = null;
			}

			if (this.globalConfig.updateCheck.consoleDelay != -1) {
				Task.builder().delayTicks(this.globalConfig.updateCheck.consoleDelay)
						.execute(() -> Sponge.getServer().getConsole().sendMessage(msg1)).submit(this);
			}

			if (this.globalConfig.updateCheck.playerDelay != -1)
				this.updateMessages = msg2 == null ? new Text[]{msg1} : new Text[]{msg1, msg2};
		}
	}

	public ConfigurationLoader<CommentedConfigurationNode> createConfigLoader(Path file) {
		return HoconConfigurationLoader.builder().setPath(file).setDefaultOptions(this.configOptions).build();
	}

	public boolean backupFile(Path file) throws IOException {
		if (!Files.exists(file))
			return false;

		String fn = file.getFileName() + ".backup";
		Path backup = null;
		for (int i = 0; i < 100; i++) {
			backup = file.resolveSibling(fn + i);
			if (!Files.exists(backup))
				break;
		}
		Files.move(file, backup);
		return true;
	}

	public Path getWorldConfigsDirectory() {
		return this.worldConfigsDir;
	}

	public Path getCacheDirectory() {
		return this.cacheDir;
	}

	public GlobalConfig.Immutable getGlobalConfig() {
		if (this.globalConfig == null)
			throw new IllegalStateException("Config not loaded");
		return this.globalConfig;
	}

	public Text[] getUpdateMessages() {
		return this.updateMessages;
	}

	public static AntiXray get() {
		if (instance == null)
			throw new IllegalStateException("Instance not available");
		return instance;
	}
}
