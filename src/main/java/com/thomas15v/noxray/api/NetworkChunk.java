/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Thomas Vanmellaerts
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

package com.thomas15v.noxray.api;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.base.Objects;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;

/**
 * Represent a chunk viewed for the network (akka online players)
 */
public class NetworkChunk {

	private NetworkBlockContainer[] blockStateContainers;
	private Chunk chunk;

	public NetworkChunk(NetworkBlockContainer[] blockStateContainers, Chunk chunk) {
		this.blockStateContainers = blockStateContainers;
		this.chunk = chunk;
	}

	public BlockState get(Vector3i vector3i) {
		return get(vector3i.getX(), vector3i.getY(), vector3i.getZ());
	}

	public BlockState get(int x, int y, int z) {
		NetworkBlockContainer blockContainer = getBlockContainerFor(y);
		if (blockContainer == null) {
			return null;
		}
		return (BlockState) blockContainer.get(x, y & 15, z);
	}

	@Nullable
	private NetworkBlockContainer getBlockContainerFor(int y) {
		return blockStateContainers[y >> 4];
	}

	public void set(Location<World> vector3i, BlockState blockState) {
		set(vector3i.getBlockX() & 15, vector3i.getBlockY(), vector3i.getBlockZ() & 15, blockState);
	}

	private void set(int x, int y, int z, BlockState blockState) {
		NetworkBlockContainer blockContainer = getBlockContainerFor(y);
		if (blockContainer != null) {
			blockContainer.set(x, y & 15, z, (IBlockState) blockState);
		}
	}

	/**
	 * Obfuscates all the known blocks inside a chunk. Since we don't know the blocks bordering the chunk yet
	 */
	public void obfuscate() {
		for (NetworkBlockContainer blockStateContainer : blockStateContainers) {
			if (blockStateContainer != null) {
				blockStateContainer.obfuscate(this);
			}
		}
	}

	public Vector3i getLocation() {
		return chunk.getPosition();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		NetworkChunk that = (NetworkChunk) o;
		return Objects.equal(blockStateContainers, that.blockStateContainers) &&
				Objects.equal(chunk, that.chunk);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(blockStateContainers, chunk);
	}

	public NetworkBlockContainer[] getBlockStateContainers() {
		return blockStateContainers;
	}

	public World getWorld() {
		return chunk.getWorld();
	}
}
