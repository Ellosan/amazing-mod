package com.ellosan.amazing.entity;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

/**
 * A city citizen — Amazing's favorite kind of customer. Citizens come in
 * several skins, live in city houses, receive ambient deliveries, and have
 * strong opinions about shipping speed.
 */
public class CitizenEntity extends PathAwareEntity {
	public static final int VARIANTS = 4;

	private static final TrackedData<Byte> VARIANT =
			DataTracker.registerData(CitizenEntity.class, TrackedDataHandlerRegistry.BYTE);

	private static final String[] NAMES = {
			"Alex Doorstep", "Sam Porchly", "Casey Blocksworth", "Riley Lobbyist",
			"Jordan Stoop", "Morgan Mailbox", "Quinn Curbside", "Avery Awning",
			"Robin Balcony", "Charlie Driveway", "Frankie Foyer", "Skyler Stairwell"
	};

	private static final String[] CHATTER = {
			"My package said 46 seconds. It took 47. Unbelievable.",
			"I ordered a single torch. It came in a box big enough for a horse.",
			"Prime is totally worth it. I ordered eight anvils yesterday.",
			"Have you tried the catalog? Press O. It changed my life.",
			"The vans honk at 6 AM but honestly? Iconic.",
			"I'm saving up for my own van. Then I'll deliver MYSELF places.",
			"A courier called Zippy Zoe once sprinted through my flowerbed.",
			"MineBank charged me $0 in fees this month. Suspicious.",
			"They say the warehouse never sleeps. I've seen the lamps. It's true."
	};

	public CitizenEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.setPersistent();
	}

	public static DefaultAttributeContainer.Builder createCitizenAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.5));
		this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0f));
		this.goalSelector.add(7, new LookAroundGoal(this));
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(VARIANT, (byte) 0);
	}

	public int getVariant() {
		return Math.floorMod(this.dataTracker.get(VARIANT), VARIANTS);
	}

	@Override
	@Nullable
	public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty,
			SpawnReason spawnReason, @Nullable EntityData entityData) {
		this.dataTracker.set(VARIANT, (byte) this.random.nextInt(VARIANTS));
		if (this.getCustomName() == null) {
			this.setCustomName(Text.literal(NAMES[this.random.nextInt(NAMES.length)]));
		}
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	@Override
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (this.getWorld().isClient) {
			return ActionResult.SUCCESS;
		}
		if (player instanceof ServerPlayerEntity) {
			String line = CHATTER[this.random.nextInt(CHATTER.length)];
			player.sendMessage(Text.literal("<" + this.getName().getString() + "> ")
					.formatted(Formatting.AQUA)
					.append(Text.literal(line).formatted(Formatting.WHITE)), false);
			this.playSound(SoundEvents.ENTITY_VILLAGER_AMBIENT, 1.0f, 1.2f);
		}
		return ActionResult.CONSUME;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putByte("Variant", this.dataTracker.get(VARIANT));
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		this.dataTracker.set(VARIANT, nbt.getByte("Variant"));
	}
}
