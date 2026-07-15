package com.ellosan.amazing.block;

import com.ellosan.amazing.entity.SeatEntity;
import com.ellosan.amazing.registry.ModEntities;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

/** A comfy chair. Right-click to sit down. */
public class ChairBlock extends HorizontalFacingBlock {
	private static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 10.0, 14.0);

	public static final MapCodec<ChairBlock> CODEC = createCodec(ChairBlock::new);

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
	}

	public ChairBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getStateManager().getDefaultState().with(FACING, Direction.NORTH));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Nullable
	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		if (player.hasVehicle()) {
			return ActionResult.PASS;
		}
		SeatEntity seat = ModEntities.SEAT.create(world);
		if (seat == null) {
			return ActionResult.PASS;
		}
		seat.setPosition(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5);
		seat.setYaw(state.get(FACING).asRotation());
		world.spawnEntity(seat);
		player.startRiding(seat);
		return ActionResult.CONSUME;
	}
}
