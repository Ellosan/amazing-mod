package com.ellosan.amazing.entity;

import com.ellosan.amazing.registry.ModItems;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

/**
 * A drivable Amazing delivery van.
 *
 * <p>Movement follows the boat pattern: the client of the controlling player
 * simulates physics locally and vanilla vehicle-move packets keep the server
 * in sync, so driving feels responsive in multiplayer. When nobody is driving
 * (or an AI subclass is in charge) the server is the logical side and runs the
 * same physics.
 */
public class VanEntity extends Entity {
	public static final float MAX_FORWARD_SPEED = 0.55f;
	public static final float MAX_REVERSE_SPEED = 0.22f;

	/** Top speed going forward, blocks/tick. Overridden by faster vehicles. */
	protected float maxForwardSpeed() {
		return MAX_FORWARD_SPEED;
	}

	/** Throttle response, blocks/tick². */
	protected float acceleration() {
		return 0.017f;
	}

	/** The item this vehicle packs back into. */
	protected ItemStack asItemStack() {
		return new ItemStack(ModItems.VAN);
	}

	// Driving inputs, set client-side by the driver (or server-side by AI).
	protected boolean pressingForward;
	protected boolean pressingBack;
	protected boolean pressingLeft;
	protected boolean pressingRight;
	protected boolean braking;

	/** Signed speed along the facing direction, blocks/tick. */
	protected float speed;

	// Wheel animation state, advanced on every side from observed movement.
	public float wheelAngle;
	public float prevWheelAngle;
	public float yawDelta;

	private float damageTaken;

	// Interpolation for clients that are not controlling this van.
	private int lerpSteps;
	private double lerpX;
	private double lerpY;
	private double lerpZ;
	private float lerpYaw;

	public VanEntity(EntityType<?> type, World world) {
		super(type, world);
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

	public void setInputs(boolean forward, boolean back, boolean left, boolean right, boolean brake) {
		this.pressingForward = forward;
		this.pressingBack = back;
		this.pressingLeft = left;
		this.pressingRight = right;
		this.braking = brake;
	}

	@Override
	public void tick() {
		super.tick();

		float prevTickYaw = this.getYaw();
		this.prevWheelAngle = this.wheelAngle;

		if (this.isLogicalSideForUpdatingMovement()) {
			this.lerpSteps = 0;
			this.drive();
			this.move(MovementType.SELF, this.getVelocity());
		} else {
			this.setVelocity(Vec3d.ZERO);
			this.interpolatePosition();
		}

		this.yawDelta = MathHelper.wrapDegrees(this.getYaw() - prevTickYaw);
		this.spinWheels();

		if (this.getWorld().isClient && this.hasPassengers()) {
			this.emitExhaust();
		}
	}

	/** Applies driving inputs, gravity and friction, producing this tick's velocity. */
	protected void drive() {
		// Throttle.
		if (this.pressingForward) {
			this.speed += this.acceleration();
		} else if (this.pressingBack) {
			this.speed -= 0.012f;
		} else {
			this.speed *= 0.95f;
		}
		if (this.braking) {
			this.speed *= 0.80f;
		}
		this.speed = MathHelper.clamp(this.speed, -MAX_REVERSE_SPEED, this.maxForwardSpeed());
		if (Math.abs(this.speed) < 0.003f) {
			this.speed = 0.0f;
		}

		// Steering: sharper at low speed, straighter at high speed, reversed in reverse.
		int turn = (this.pressingRight ? 1 : 0) - (this.pressingLeft ? 1 : 0);
		if (turn != 0 && Math.abs(this.speed) > 0.02f) {
			float agility = MathHelper.clamp(Math.abs(this.speed) / 0.12f, 0.0f, 1.0f);
			this.setYaw(this.getYaw() + turn * 3.4f * agility * Math.signum(this.speed));
		}

		Vec3d forward = Vec3d.fromPolar(0.0f, this.getYaw());
		double velocityY = this.getVelocity().y;

		if (this.isTouchingWater()) {
			// Vans are (surprisingly) buoyant, but slow in water.
			velocityY = Math.min(velocityY + 0.06, 0.08);
			this.speed *= 0.9f;
		} else if (!this.isOnGround()) {
			velocityY -= 0.08;
		} else {
			velocityY = Math.max(velocityY, -0.1);
		}

		// Hop up single blocks when driving into them.
		if (this.horizontalCollision && this.isOnGround() && Math.abs(this.speed) > 0.05f) {
			velocityY = 0.42;
		}

		this.setVelocity(forward.x * this.speed, velocityY, forward.z * this.speed);
		this.fallDistance = 0.0f;
	}

	private void interpolatePosition() {
		if (this.lerpSteps > 0) {
			this.setPosition(
					this.getX() + (this.lerpX - this.getX()) / this.lerpSteps,
					this.getY() + (this.lerpY - this.getY()) / this.lerpSteps,
					this.getZ() + (this.lerpZ - this.getZ()) / this.lerpSteps);
			this.setYaw(this.getYaw() + MathHelper.wrapDegrees(this.lerpYaw - this.getYaw()) / this.lerpSteps);
			this.lerpSteps--;
		}
	}

	private void spinWheels() {
		double dx = this.getX() - this.prevX;
		double dz = this.getZ() - this.prevZ;
		float distance = (float) Math.sqrt(dx * dx + dz * dz);
		Vec3d forward = Vec3d.fromPolar(0.0f, this.getYaw());
		float direction = (dx * forward.x + dz * forward.z) >= 0 ? 1.0f : -1.0f;
		this.wheelAngle += direction * distance * 2.2f;
	}

	private void emitExhaust() {
		if (Math.abs(this.speed) > 0.04f && this.age % 3 == 0) {
			Vec3d back = Vec3d.fromPolar(0.0f, this.getYaw()).multiply(-1.4);
			this.getWorld().addParticle(ParticleTypes.SMOKE,
					this.getX() + back.x, this.getY() + 0.25, this.getZ() + back.z,
					0.0, 0.02, 0.0);
		}
	}

	@Override
	public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
		this.lerpX = x;
		this.lerpY = y;
		this.lerpZ = z;
		this.lerpYaw = yaw;
		this.lerpSteps = 10;
	}

	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		if (player.shouldCancelInteraction()) {
			return ActionResult.PASS;
		}
		if (!this.getWorld().isClient) {
			return player.startRiding(this) ? ActionResult.CONSUME : ActionResult.PASS;
		}
		return ActionResult.SUCCESS;
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (this.getWorld().isClient || this.isRemoved()) {
			return false;
		}
		if (!(source.getAttacker() instanceof PlayerEntity player)) {
			return false;
		}

		this.scheduleVelocityUpdate();
		this.damageTaken += amount + 9.0f;
		this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.ENTITY_IRON_GOLEM_HURT,
				SoundCategory.NEUTRAL, 0.8f, 1.3f);

		if (player.getAbilities().creativeMode || this.damageTaken > 25.0f) {
			this.breakIntoItem(!player.getAbilities().creativeMode);
		}
		return true;
	}

	protected void breakIntoItem(boolean dropItem) {
		if (dropItem) {
			this.dropStack(this.asItemStack());
		}
		this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.ENTITY_IRON_GOLEM_DEATH,
				SoundCategory.NEUTRAL, 0.7f, 1.4f);
		this.discard();
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return this.getPassengerList().size() < 2;
	}

	@Override
	protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
		if (!this.hasPassenger(passenger)) {
			return;
		}
		// Driver sits toward the front, a second passenger in the cargo bay.
		double forwardOffset = this.getPassengerList().indexOf(passenger) == 0 ? 0.35 : -0.7;
		Vec3d forward = Vec3d.fromPolar(0.0f, this.getYaw()).multiply(forwardOffset);
		positionUpdater.accept(passenger,
				this.getX() + forward.x,
				this.getY() + 0.72,
				this.getZ() + forward.z);
	}

	@Nullable
	@Override
	public LivingEntity getControllingPassenger() {
		return this.getFirstPassenger() instanceof PlayerEntity player ? player : null;
	}

	@Override
	public boolean canHit() {
		return !this.isRemoved();
	}

	@Override
	public boolean isCollidable() {
		return true;
	}

	@Override
	public ItemStack getPickBlockStack() {
		return this.asItemStack();
	}

	/** Current signed speed, for engine sounds and animation. */
	public float getSpeed() {
		return this.speed;
	}
}
