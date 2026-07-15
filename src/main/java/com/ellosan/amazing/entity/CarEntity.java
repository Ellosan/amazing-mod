package com.ellosan.amazing.entity;

import com.ellosan.amazing.registry.ModItems;

import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** The Amazing Roadster — smaller, faster, redder. */
public class CarEntity extends VanEntity {

	public CarEntity(EntityType<?> type, World world) {
		super(type, world);
	}

	@Override
	protected float maxForwardSpeed() {
		return 0.85f;
	}

	@Override
	protected float acceleration() {
		return 0.024f;
	}

	@Override
	protected ItemStack asItemStack() {
		return new ItemStack(ModItems.CAR);
	}
}
