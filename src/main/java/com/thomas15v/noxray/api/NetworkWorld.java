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

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Represent the world viewed for the network (akka online players)
 */
public class NetworkWorld {
	// Thread-safe map
	private Map<Vector3i, NetworkChunk> chunks = new ConcurrentSkipListMap<>();
	//todo: modifiers for each world
	private BlockModifier modifier;

	public void addChunk(NetworkChunk chunk) {
		this.chunks.put(chunk.getPosition(), chunk);
	}

	public void removeChunk(Vector3i pos) {
		this.chunks.remove(pos);
	}

	@Nullable
	private NetworkChunk getChunk(Vector3i pos) {
		return this.chunks.get(pos);
	}

	private Collection<NetworkChunk> getChunks() {
		return this.chunks.values();
	}

	public BlockModifier getModifier() {
		return this.modifier;
	}

	public void setModifier(BlockModifier modifier) {
		this.modifier = modifier;
	}
}
