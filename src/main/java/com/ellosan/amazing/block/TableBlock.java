package com.ellosan.amazing.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/** A sturdy table. Holds snacks, redstone projects, and elbows. */
public class TableBlock extends Block {
	private static final VoxelShape SHAPE = VoxelShapes.union(
			Block.createCuboidShape(0.0, 12.0, 0.0, 16.0, 16.0, 16.0),
			Block.createCuboidShape(6.0, 0.0, 6.0, 10.0, 12.0, 10.0));

	public TableBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}
}
