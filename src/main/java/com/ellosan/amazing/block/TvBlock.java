package com.ellosan.amazing.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

/** A flatscreen TV. Right-click to zap between AMZ News and off. */
public class TvBlock extends HorizontalFacingBlock {
	public static final BooleanProperty ON = Properties.LIT;

	private static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 5.0, 16.0, 12.0, 11.0);

	public static final MapCodec<TvBlock> CODEC = createCodec(TvBlock::new);

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
	}

	public TvBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.getStateManager().getDefaultState()
				.with(FACING, Direction.NORTH).with(ON, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, ON);
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
		boolean nowOn = !state.get(ON);
		world.setBlockState(pos, state.with(ON, nowOn));
		world.playSound(null, pos, nowOn ? SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME : SoundEvents.BLOCK_LEVER_CLICK,
				SoundCategory.BLOCKS, 0.7f, nowOn ? 1.4f : 0.7f);
		return ActionResult.success(world.isClient);
	}
}
