/*
 * Copyright (c) 2018-2021 Hugo Dupanloup (Yeregorix)
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

package net.smoofyuniverse.mirage.mixin.server;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.world.WorldServer;
import net.smoofyuniverse.mirage.impl.internal.InternalChunkMap;
import net.smoofyuniverse.mirage.impl.internal.InternalWorld;
import net.smoofyuniverse.mirage.impl.network.NetworkWorld;
import net.smoofyuniverse.mirage.impl.network.dynamic.DynamicWorld;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mixin(PlayerChunkMap.class)
public class PlayerChunkMapMixin implements InternalChunkMap {

	@Shadow
	@Final
	private WorldServer world;

	private final Map<UUID, DynamicWorld> dynamicWorlds = new HashMap<>();

	@Inject(method = "removePlayer", at = @At(value = "INVOKE", target = "Ljava/util/List;remove(Ljava/lang/Object;)Z"))
	public void onRemovePlayer(EntityPlayerMP player, CallbackInfo ci) {
		if (isDynamismEnabled())
			removeDynamicWorld(player.getUniqueID());
	}

	private NetworkWorld getView() {
		return ((InternalWorld) this.world).getView();
	}

	@Inject(method = "updateMovingPlayer", at = @At("RETURN"))
	public void onUpdateMovingPlayer(EntityPlayerMP player, CallbackInfo ci) {
		if (isDynamismEnabled())
			getDynamicWorld(player.getUniqueID()).ifPresent(DynamicWorld::updateCenter);
	}

	@Override
	public boolean isDynamismEnabled() {
		return getView().isDynamismEnabled();
	}

	@Override
	public DynamicWorld getOrCreateDynamicWorld(Player player) {
		DynamicWorld dynWorld = this.dynamicWorlds.get(player.getUniqueId());
		if (dynWorld == null) {
			if (!isDynamismEnabled())
				throw new UnsupportedOperationException();

			dynWorld = new DynamicWorld(getView(), player);
			dynWorld.updateCenter();
			this.dynamicWorlds.put(player.getUniqueId(), dynWorld);
		}
		return dynWorld;
	}

	@Override
	public Optional<DynamicWorld> getDynamicWorld(UUID id) {
		return Optional.ofNullable(this.dynamicWorlds.get(id));
	}

	@Override
	public void removeDynamicWorld(UUID id) {
		this.dynamicWorlds.remove(id);
	}


}
