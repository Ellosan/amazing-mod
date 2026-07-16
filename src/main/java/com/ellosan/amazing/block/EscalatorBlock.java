package com.ellosan.amazing.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

/**
 * A moving stair step. Stand on it and it carries you along its facing and
 * gently upward — chain them diagonally for a full escalator. Walking is
 * for ground floors.
 */
public class EscalatorBlock extends HorizontalFacingBlock {
	private static final VoxelShape SHAPE = VoxelShapes.union(
			Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 8.0, 16.0),
			Block.createCuboidShape(0.0, 8.0, 8.0, 16.0, 16.0, 16.0));

	public static final MapCodec<EscalatorBlock> CODEC = createCodec(EscalatorBlock::new);

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
	}

	public EscalatorBlock(Settings settings) {
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
		// The step carries you AWAY from where you're looking as you board it.
		return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing());
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		if (!(entity instanceof LivingEntity) || entity.isSneaking()) {
			return;
		}
		Direction direction = state.get(FACING);
		Vec3d velocity = entity.getVelocity();
		double push = 0.18;
		entity.setVelocity(
				velocity.x * 0.5 + direction.getOffsetX() * push,
				Math.max(velocity.y, 0.25),
				velocity.z * 0.5 + direction.getOffsetZ() * push);
		entity.velocityDirty = true;
		entity.fallDistance = 0.0f;
	}
}
