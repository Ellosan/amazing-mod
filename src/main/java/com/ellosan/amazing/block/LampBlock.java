package com.ellosan.amazing.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** A standing lamp. Right-click to flip the switch. */
public class LampBlock extends Block {
	public static final BooleanProperty LIT = Properties.LIT;

	private static final VoxelShape SHAPE = VoxelShapes.union(
			Block.createCuboidShape(5.0, 0.0, 5.0, 11.0, 2.0, 11.0),
			Block.createCuboidShape(7.0, 2.0, 7.0, 9.0, 10.0, 9.0),
			Block.createCuboidShape(4.0, 10.0, 4.0, 12.0, 16.0, 12.0));

	public LampBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getStateManager().getDefaultState().with(LIT, true));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		world.setBlockState(pos, state.cycle(LIT));
		world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.5f,
				state.get(LIT) ? 0.6f : 1.0f);
		return ActionResult.success(world.isClient);
	}
}
