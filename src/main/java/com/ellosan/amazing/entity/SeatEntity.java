package com.ellosan.amazing.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

/** Invisible rideable pin that lets players sit on furniture. */
public class SeatEntity extends Entity {

	public SeatEntity(EntityType<?> type, World world) {
		super(type, world);
		this.noClip = true;
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.getWorld().isClient
				&& (this.getPassengerList().isEmpty() || this.getWorld().getBlockState(this.getBlockPos()).isAir())) {
			this.discard();
		}
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return this.getPassengerList().isEmpty();
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
	}
}
