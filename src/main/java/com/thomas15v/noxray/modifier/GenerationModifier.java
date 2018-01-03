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

package com.thomas15v.noxray.modifier;

import com.thomas15v.noxray.api.BlockModifier;
import com.thomas15v.noxray.modifications.OreUtil;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Only modifies the blocks on disk read and so async. Causing almost no lag, but might increase length on the chunkloading (not that that is a problem really)
 */
public class GenerationModifier implements BlockModifier {

	private static final List<BlockType> COMMON_BLOCKS = Arrays.asList(BlockTypes.AIR, BlockTypes.STONE, BlockTypes.NETHERRACK, BlockTypes.END_STONE, BlockTypes.BEDROCK);
	private static final Predicate<BlockState> FILTER = blockState -> OreUtil.isOre(blockState.getType());

	@Override
	public BlockState handleBlock(BlockState original, Location<World> location, List<BlockState> surroundingBlocks) {
		for (BlockState surroundingBlock : surroundingBlocks) {
			if (surroundingBlock.getType().equals(BlockTypes.AIR)) {
				if (surroundingBlock.getType().equals(BlockTypes.WATER)) {
					return original;
				} else {
					return handlePlayerBlock(original, location);
				}
			}
		}
		return BlockTypes.STONE.getDefaultState();

	}

	public BlockState handlePlayerBlock(BlockState original, Location<World> location) {
		return original;
	}

	@Override
	public Predicate<BlockState> getFilter() {
		return FILTER;
	}


	public static boolean checkBlock(Location blockState, Direction direction) {
		return !blockState.getBlockRelative(direction).getBlock().getType().equals(BlockTypes.AIR);
	}
}
