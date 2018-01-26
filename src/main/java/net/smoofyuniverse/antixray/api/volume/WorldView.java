/*
 * The MIT License (MIT)
 *
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

package net.smoofyuniverse.antixray.api.volume;

import com.flowpowered.math.vector.Vector3i;
import net.smoofyuniverse.antixray.api.modifier.ChunkModifier;
import net.smoofyuniverse.antixray.config.WorldConfig;
import org.spongepowered.api.util.Identifiable;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Map;
import java.util.Optional;

/**
 * Represents a mutable client-side world.
 */
public interface WorldView extends BlockView, Identifiable {

	/**
	 * @return The WorldStorage which is associated with this WorldView
	 */
	@Override
	WorldStorage getStorage();

	/**
	 * @return The name of this world
	 */
	String getName();

	/**
	 * @return The properties of this world
	 */
	WorldProperties getProperties();

	Map<ChunkModifier, Object> getModifiers();

	WorldConfig.Immutable getConfig();

	default boolean isChunkLoaded(Vector3i pos) {
		return isChunkLoaded(pos.getX(), pos.getY(), pos.getZ());
	}

	default boolean isChunkLoaded(int x, int y, int z) {
		return getChunkView(x, y, z).isPresent();
	}

	Optional<ChunkView> getChunkView(int x, int y, int z);

	default Optional<ChunkView> getChunkView(Vector3i pos) {
		return getChunkView(pos.getX(), pos.getY(), pos.getZ());
	}

	default Optional<ChunkView> getChunkViewAt(Vector3i pos) {
		return getChunkView(pos.getX(), pos.getY(), pos.getZ());
	}

	Optional<ChunkView> getChunkViewAt(int x, int y, int z);

	default boolean deobfuscate(Vector3i pos) {
		return deobfuscate(pos.getX(), pos.getY(), pos.getZ());
	}

	boolean deobfuscate(int x, int y, int z);
}
