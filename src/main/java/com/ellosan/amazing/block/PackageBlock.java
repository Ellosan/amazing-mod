package com.ellosan.amazing.block;

import com.ellosan.amazing.delivery.DeliveryManager;
import com.ellosan.amazing.shop.ProductCatalog;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A sealed Amazing package block, as stacked high in every warehouse.
 * Breaking one yields a package item with a random product inside.
 */
public class PackageBlock extends Block {

	public PackageBlock(Settings settings) {
		super(settings);
	}

	@Override
	public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state,
			@Nullable BlockEntity blockEntity, ItemStack tool) {
		super.afterBreak(world, player, pos, state, blockEntity, tool);
		if (world instanceof ServerWorld serverWorld && !player.getAbilities().creativeMode) {
			ItemStack packageStack = DeliveryManager.buildPackage(
					List.of(ProductCatalog.random(serverWorld.random).createStack()), "Warehouse stock");
			Block.dropStack(world, pos, packageStack);
		}
	}
}
