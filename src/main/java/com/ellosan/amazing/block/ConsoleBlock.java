package com.ellosan.amazing.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

/**
 * The GameCube-est block around: the Amazing GameBox. Right-click to start a
 * gaming session — it hums, blinks, and occasionally wins.
 */
public class ConsoleBlock extends HorizontalFacingBlock {
	public static final BooleanProperty ON = Properties.LIT;

	private static final VoxelShape SHAPE = Block.createCuboidShape(4.0, 0.0, 4.0, 12.0, 4.0, 12.0);

	public static final MapCodec<ConsoleBlock> CODEC = createCodec(ConsoleBlock::new);

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
	}

	public ConsoleBlock(Settings settings) {
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
		world.playSound(null, pos,
				nowOn ? SoundEvents.BLOCK_NOTE_BLOCK_PLING.value() : SoundEvents.BLOCK_LEVER_CLICK,
				SoundCategory.BLOCKS, 0.7f, nowOn ? 1.2f : 0.7f);
		// Turning on a nearby TV completes the setup.
		if (nowOn) {
			for (BlockPos nearby : BlockPos.iterate(pos.add(-3, -1, -3), pos.add(3, 2, 3))) {
				BlockState nearbyState = world.getBlockState(nearby);
				if (nearbyState.getBlock() instanceof TvBlock && !nearbyState.get(TvBlock.ON)) {
					world.setBlockState(nearby, nearbyState.with(TvBlock.ON, true));
					break;
				}
			}
		}
		return ActionResult.success(world.isClient);
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (state.get(ON)) {
			world.addParticle(ParticleTypes.NOTE,
					pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
					random.nextDouble(), 0.0, 0.0);
		}
	}

	@Override
	public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (state.get(ON)) {
			world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(),
					SoundCategory.BLOCKS, 0.3f, 0.8f + random.nextFloat());
		}
	}
}
