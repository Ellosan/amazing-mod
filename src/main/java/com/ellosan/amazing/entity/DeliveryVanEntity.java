package com.ellosan.amazing.entity;

import com.ellosan.amazing.registry.ModEntities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * An autonomous Amazing delivery van. It drives itself to the customer,
 * drops off a courier who hands over the package, waits for them to hop
 * back in, then drives off into the sunset and despawns.
 */
public class DeliveryVanEntity extends VanEntity {
	private static final int DRIVE_TIMEOUT_TICKS = 20 * 45;
	private static final int WAIT_TIMEOUT_TICKS = 20 * 60;
	private static final int LEAVE_TICKS = 20 * 6;

	private enum Phase {
		DRIVING_IN,
		WAITING,
		LEAVING
	}

	private Phase phase = Phase.DRIVING_IN;
	private int phaseTicks;
	private int stuckTicks;

	@Nullable
	private UUID customerUuid;
	private BlockPos fallbackDestination = BlockPos.ORIGIN;
	private ItemStack packageStack = ItemStack.EMPTY;

	public DeliveryVanEntity(EntityType<?> type, World world) {
		super(type, world);
	}

	public void setDelivery(UUID customerUuid, BlockPos fallbackDestination, ItemStack packageStack) {
		this.customerUuid = customerUuid;
		this.fallbackDestination = fallbackDestination;
		this.packageStack = packageStack;
	}

	/** Called by the courier once they are back in the van. */
	public void startLeaving() {
		this.phase = Phase.LEAVING;
		this.phaseTicks = 0;
	}

	@Override
	public void tick() {
		if (!this.getWorld().isClient) {
			this.phaseTicks++;
			switch (this.phase) {
				case DRIVING_IN -> this.tickDrivingIn();
				case WAITING -> this.tickWaiting();
				case LEAVING -> this.tickLeaving();
			}
		}
		super.tick();
	}

	private Vec3d destination() {
		Entity customer = this.findCustomer();
		if (customer != null) {
			return customer.getPos();
		}
		return Vec3d.ofBottomCenter(this.fallbackDestination);
	}

	@Nullable
	private Entity findCustomer() {
		if (this.customerUuid != null && this.getWorld() instanceof ServerWorld serverWorld) {
			return serverWorld.getEntity(this.customerUuid);
		}
		return null;
	}

	private void tickDrivingIn() {
		Vec3d destination = this.destination();
		double dx = destination.x - this.getX();
		double dz = destination.z - this.getZ();
		double distance = Math.sqrt(dx * dx + dz * dz);

		if (distance < 5.0 || this.phaseTicks > DRIVE_TIMEOUT_TICKS || this.stuckTicks > 100) {
			this.arrive();
			return;
		}

		this.steerTowards(dx, dz);
		boolean slowDown = distance < 10.0;
		this.setInputs(!slowDown || this.getSpeed() < 0.18f, false, false, false, slowDown && this.getSpeed() > 0.2f);

		// Track whether we are actually making progress.
		double moved = Math.abs(this.getX() - this.prevX) + Math.abs(this.getZ() - this.prevZ);
		if (moved < 0.02) {
			this.stuckTicks++;
		} else {
			this.stuckTicks = Math.max(0, this.stuckTicks - 2);
		}
	}

	/** Turns the wheel toward the destination; actual yaw change happens in drive(). */
	private void steerTowards(double dx, double dz) {
		float desiredYaw = (float) (MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
		float error = MathHelper.wrapDegrees(desiredYaw - this.getYaw());
		// AI gets to cheat a little and rotate directly for reliable navigation.
		this.setYaw(this.getYaw() + MathHelper.clamp(error, -4.5f, 4.5f));
	}

	private void arrive() {
		this.setInputs(false, false, false, false, true);
		this.phase = Phase.WAITING;
		this.phaseTicks = 0;

		if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		serverWorld.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_IRON_DOOR_OPEN,
				SoundCategory.NEUTRAL, 0.8f, 1.3f);

		DeliveryWorkerEntity worker = ModEntities.DELIVERY_WORKER.create(serverWorld);
		if (worker == null) {
			this.startLeaving();
			return;
		}
		Vec3d side = Vec3d.fromPolar(0.0f, this.getYaw() + 90.0f).multiply(1.4);
		worker.refreshPositionAndAngles(this.getX() + side.x, this.getY() + 0.2, this.getZ() + side.z,
				this.getYaw(), 0.0f);
		worker.startDelivery(this.customerUuid, this.fallbackDestination, this.packageStack, this.getUuid());
		serverWorld.spawnEntity(worker);

		this.packageStack = ItemStack.EMPTY;
	}

	private void tickWaiting() {
		this.setInputs(false, false, false, false, true);
		if (this.phaseTicks > WAIT_TIMEOUT_TICKS) {
			this.startLeaving();
		}
	}

	private void tickLeaving() {
		this.setInputs(true, false, false, false, false);
		if (this.phaseTicks > LEAVE_TICKS) {
			if (this.getWorld() instanceof ServerWorld serverWorld) {
				serverWorld.spawnParticles(ParticleTypes.CLOUD,
						this.getX(), this.getY() + 1.0, this.getZ(), 20, 1.0, 0.6, 1.0, 0.02);
				serverWorld.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
						SoundCategory.NEUTRAL, 0.6f, 0.8f);
			}
			this.dropCarriedPackage();
			this.discard();
		}
	}

	private void dropCarriedPackage() {
		// Never destroy a customer's order: if the courier could not deliver it,
		// leave it on the road.
		if (!this.packageStack.isEmpty()) {
			this.dropStack(this.packageStack);
			this.packageStack = ItemStack.EMPTY;
		}
	}

	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		// Working vehicle — no joyriders.
		return ActionResult.PASS;
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (this.getWorld().isClient || this.isRemoved()) {
			return false;
		}
		if (source.getAttacker() instanceof PlayerEntity) {
			this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.ENTITY_IRON_GOLEM_HURT,
					SoundCategory.NEUTRAL, 0.8f, 1.3f);
			// Vandalising the delivery van just makes it leave (with the goods).
			this.startLeaving();
			return true;
		}
		return false;
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (this.customerUuid != null) {
			nbt.putUuid("Customer", this.customerUuid);
		}
		nbt.putLong("Destination", this.fallbackDestination.asLong());
		nbt.putString("Phase", this.phase.name());
		if (!this.packageStack.isEmpty()) {
			nbt.put("Package", this.packageStack.encode(this.getRegistryManager()));
		}
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.containsUuid("Customer")) {
			this.customerUuid = nbt.getUuid("Customer");
		}
		this.fallbackDestination = BlockPos.fromLong(nbt.getLong("Destination"));
		try {
			this.phase = Phase.valueOf(nbt.getString("Phase"));
		} catch (IllegalArgumentException e) {
			this.phase = Phase.DRIVING_IN;
		}
		if (nbt.contains("Package", NbtElement.COMPOUND_TYPE)) {
			this.packageStack = ItemStack.fromNbt(this.getRegistryManager(), nbt.getCompound("Package"))
					.orElse(ItemStack.EMPTY);
		}
	}
}
