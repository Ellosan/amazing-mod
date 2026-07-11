package com.ellosan.amazing.entity;

import com.ellosan.amazing.quest.QuestManager;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * An Amazing courier. Either on a mission (carry the package from the van to
 * the customer, then jog back) or idling around as a friendly quest giver.
 */
public class DeliveryWorkerEntity extends PathAwareEntity {
	private static final String[] NAMES = {
			"Driver Dave", "Speedy Sam", "Parcel Pat", "Boxer Bella", "Turbo Tina",
			"Hasty Hans", "Zippy Zoe", "Courier Carl", "Dash Dana", "Miles Munroe"
	};

	private static final int PHASE_TIMEOUT_TICKS = 20 * 40;

	private enum Mission {
		NONE,
		DELIVERING,
		RETURNING
	}

	private Mission mission = Mission.NONE;
	private int missionTicks;

	@Nullable
	private UUID customerUuid;
	@Nullable
	private UUID vanUuid;
	private BlockPos fallbackDestination = BlockPos.ORIGIN;
	private ItemStack packageStack = ItemStack.EMPTY;

	public DeliveryWorkerEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
	}

	public static DefaultAttributeContainer.Builder createWorkerAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.42)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.5));
		this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0f));
		this.goalSelector.add(7, new LookAroundGoal(this));
	}

	public void startDelivery(@Nullable UUID customerUuid, BlockPos fallbackDestination,
			ItemStack packageStack, @Nullable UUID vanUuid) {
		this.mission = Mission.DELIVERING;
		this.missionTicks = 0;
		this.customerUuid = customerUuid;
		this.fallbackDestination = fallbackDestination;
		this.packageStack = packageStack;
		this.vanUuid = vanUuid;
		this.equipStack(EquipmentSlot.MAINHAND, packageStack.copy());
		if (this.getCustomName() == null) {
			this.setCustomName(Text.literal(NAMES[this.random.nextInt(NAMES.length)]));
		}
		this.setPersistent();
	}

	public boolean isOnMission() {
		return this.mission != Mission.NONE;
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		if (this.mission == Mission.NONE) {
			return;
		}

		this.missionTicks++;
		switch (this.mission) {
			case DELIVERING -> this.tickDelivering();
			case RETURNING -> this.tickReturning();
			default -> {
			}
		}
	}

	private void tickDelivering() {
		Entity customer = this.findEntity(this.customerUuid);
		Vec3d destination = customer != null ? customer.getPos() : Vec3d.ofBottomCenter(this.fallbackDestination);

		if (this.missionTicks > PHASE_TIMEOUT_TICKS) {
			this.finishDelivery(customer, true);
			return;
		}

		double distanceSq = this.squaredDistanceTo(destination.x, destination.y, destination.z);
		if (distanceSq < 2.4 * 2.4) {
			this.finishDelivery(customer, false);
			return;
		}

		if (this.age % 10 == 0) {
			this.getNavigation().startMovingTo(destination.x, destination.y, destination.z, 1.05);
		}
		if (customer != null) {
			this.getLookControl().lookAt(customer);
		}
	}

	private void finishDelivery(@Nullable Entity customer, boolean timedOut) {
		if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}

		this.swingHand(Hand.MAIN_HAND);
		this.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

		if (customer instanceof ServerPlayerEntity player) {
			if (!this.packageStack.isEmpty()) {
				if (!player.giveItemStack(this.packageStack)) {
					this.dropStack(this.packageStack);
				}
			}
			player.sendMessage(Text.literal("[Amazing] ").formatted(Formatting.GOLD)
					.append(Text.literal(this.getName().getString() + " delivered your package. Enjoy!")
							.formatted(Formatting.YELLOW)), false);
			serverWorld.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP,
					SoundCategory.NEUTRAL, 0.6f, 1.5f);
			QuestManager.onDeliveryReceived(player);
		} else if (customer instanceof VillagerEntity villager) {
			// Villagers unbox on the spot and are delighted about it.
			serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
					villager.getX(), villager.getY() + 1.5, villager.getZ(), 10, 0.4, 0.4, 0.4, 0.0);
			villager.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
			this.packageStack = ItemStack.EMPTY;
		} else if (!this.packageStack.isEmpty() && !timedOut) {
			this.dropStack(this.packageStack);
			this.packageStack = ItemStack.EMPTY;
		}

		// Anything undelivered on a timeout gets left at the doorstep.
		if (!this.packageStack.isEmpty() && customer == null) {
			this.dropStack(this.packageStack);
		}
		this.packageStack = ItemStack.EMPTY;

		serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
				this.getX(), this.getY() + 1.0, this.getZ(), 6, 0.3, 0.3, 0.3, 0.0);

		this.mission = Mission.RETURNING;
		this.missionTicks = 0;
	}

	private void tickReturning() {
		Entity van = this.findEntity(this.vanUuid);
		if (van == null || this.missionTicks > PHASE_TIMEOUT_TICKS) {
			this.leaveWithoutVan();
			return;
		}

		if (this.squaredDistanceTo(van) < 2.6 * 2.6) {
			if (van instanceof DeliveryVanEntity deliveryVan) {
				deliveryVan.startLeaving();
			}
			this.discard();
			return;
		}

		if (this.age % 10 == 0) {
			this.getNavigation().startMovingTo(van.getX(), van.getY(), van.getZ(), 1.05);
		}
	}

	private void leaveWithoutVan() {
		if (this.getWorld() instanceof ServerWorld serverWorld) {
			serverWorld.spawnParticles(ParticleTypes.CLOUD,
					this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.4, 0.6, 0.4, 0.02);
		}
		this.discard();
	}

	@Override
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (this.getWorld().isClient) {
			return ActionResult.SUCCESS;
		}
		if (player instanceof ServerPlayerEntity serverPlayer) {
			QuestManager.onWorkerInteract(serverPlayer, this);
		}
		return ActionResult.CONSUME;
	}

	@Override
	public boolean cannotDespawn() {
		return this.isOnMission() || super.cannotDespawn();
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		// Couriers are insured, unflappable, and unionised: no damage while working.
		if (this.isOnMission() && source.getAttacker() instanceof PlayerEntity) {
			this.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
			return false;
		}
		return super.damage(source, amount);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putString("Mission", this.mission.name());
		if (this.customerUuid != null) {
			nbt.putUuid("Customer", this.customerUuid);
		}
		if (this.vanUuid != null) {
			nbt.putUuid("Van", this.vanUuid);
		}
		nbt.putLong("Destination", this.fallbackDestination.asLong());
		if (!this.packageStack.isEmpty()) {
			nbt.put("Package", this.packageStack.encode(this.getRegistryManager()));
		}
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		try {
			this.mission = Mission.valueOf(nbt.getString("Mission"));
		} catch (IllegalArgumentException e) {
			this.mission = Mission.NONE;
		}
		if (nbt.containsUuid("Customer")) {
			this.customerUuid = nbt.getUuid("Customer");
		}
		if (nbt.containsUuid("Van")) {
			this.vanUuid = nbt.getUuid("Van");
		}
		this.fallbackDestination = BlockPos.fromLong(nbt.getLong("Destination"));
		if (nbt.contains("Package", NbtElement.COMPOUND_TYPE)) {
			this.packageStack = ItemStack.fromNbt(this.getRegistryManager(), nbt.getCompound("Package"))
					.orElse(ItemStack.EMPTY);
		}
	}

	@Nullable
	private Entity findEntity(@Nullable UUID uuid) {
		if (uuid != null && this.getWorld() instanceof ServerWorld serverWorld) {
			return serverWorld.getEntity(uuid);
		}
		return null;
	}
}
