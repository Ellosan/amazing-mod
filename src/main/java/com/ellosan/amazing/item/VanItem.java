package com.ellosan.amazing.item;

import com.ellosan.amazing.entity.VanEntity;

import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Supplier;

/**
 * Deploys a personal Amazing vehicle onto the clicked block.
 * Used for both the van and the roadster.
 */
public class VanItem extends Item {
	private final Supplier<EntityType<? extends VanEntity>> entityType;

	public VanItem(Supplier<EntityType<? extends VanEntity>> entityType, Settings settings) {
		super(settings);
		this.entityType = entityType;
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}

		BlockPos pos = context.getBlockPos();
		if (context.getSide() != Direction.UP) {
			return ActionResult.PASS;
		}

		VanEntity vehicle = this.entityType.get().create(world);
		if (vehicle == null) {
			return ActionResult.FAIL;
		}

		float yaw = context.getPlayer() != null ? context.getPlayer().getYaw() : 0.0f;
		vehicle.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, yaw, 0.0f);

		if (!world.isSpaceEmpty(vehicle, vehicle.getBoundingBox())) {
			vehicle.discard();
			if (context.getPlayer() != null) {
				context.getPlayer().sendMessage(Text.literal("Not enough room to park here!")
						.formatted(Formatting.RED), true);
			}
			return ActionResult.FAIL;
		}

		world.spawnEntity(vehicle);
		world.playSound(null, pos, SoundEvents.ENTITY_IRON_GOLEM_STEP, SoundCategory.NEUTRAL, 1.0f, 0.7f);

		ItemStack stack = context.getStack();
		if (context.getPlayer() == null || !context.getPlayer().getAbilities().creativeMode) {
			stack.decrement(1);
		}
		return ActionResult.CONSUME;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Place on the ground, hop in, and drive!").formatted(Formatting.GOLD));
		tooltip.add(Text.literal("WASD to drive, Space to brake, H to honk").formatted(Formatting.GRAY));
		tooltip.add(Text.literal("Punch it a few times to pack it back up").formatted(Formatting.DARK_GRAY));
	}
}
