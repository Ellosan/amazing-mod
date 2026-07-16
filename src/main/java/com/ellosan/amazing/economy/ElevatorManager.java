package com.ellosan.amazing.economy;

import com.ellosan.amazing.registry.ModBlocks;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Runs the Amazing Express Elevators: stand on an elevator pad, JUMP to go
 * up to the next pad, SNEAK to go down. Whoosh particles included at no
 * extra charge.
 */
public final class ElevatorManager {
	private static final int MAX_TRAVEL = 24;
	private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

	private ElevatorManager() {
	}

	public static void tick(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			ServerWorld world = player.getServerWorld();
			BlockPos below = player.getBlockPos().down();
			if (!world.getBlockState(below).isOf(ModBlocks.ELEVATOR)) {
				continue;
			}

			long now = world.getTime();
			long readyAt = COOLDOWNS.getOrDefault(player.getUuid(), 0L);
			if (now < readyAt) {
				continue;
			}

			if (player.getVelocity().y > 0.1) {
				travel(world, player, below, 1, now);
			} else if (player.isSneaking()) {
				travel(world, player, below, -1, now);
			}
		}
	}

	private static void travel(ServerWorld world, ServerPlayerEntity player, BlockPos from, int direction, long now) {
		for (int dy = 2; dy <= MAX_TRAVEL; dy++) {
			BlockPos candidate = from.up(direction * dy);
			if (!world.getBlockState(candidate).isOf(ModBlocks.ELEVATOR)) {
				continue;
			}
			// Needs two air blocks above the destination pad to stand in.
			if (!world.getBlockState(candidate.up()).getCollisionShape(world, candidate.up()).isEmpty()
					|| !world.getBlockState(candidate.up(2)).getCollisionShape(world, candidate.up(2)).isEmpty()) {
				continue;
			}

			COOLDOWNS.put(player.getUuid(), now + 10);
			world.spawnParticles(ParticleTypes.END_ROD,
					player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.3, 0.8, 0.3, 0.05);
			player.teleport(world, candidate.getX() + 0.5, candidate.getY() + 1.0, candidate.getZ() + 0.5,
					player.getYaw(), player.getPitch());
			world.spawnParticles(ParticleTypes.END_ROD,
					player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.3, 0.8, 0.3, 0.05);
			world.playSound(null, candidate, SoundEvents.ENTITY_ENDERMAN_TELEPORT,
					SoundCategory.BLOCKS, 0.5f, direction > 0 ? 1.6f : 1.2f);
			player.fallDistance = 0.0f;
			return;
		}
	}
}
