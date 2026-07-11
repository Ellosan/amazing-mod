package com.ellosan.amazing.registry;

import com.ellosan.amazing.AmazingMod;
import com.ellosan.amazing.entity.DeliveryVanEntity;
import com.ellosan.amazing.entity.DeliveryWorkerEntity;
import com.ellosan.amazing.entity.VanEntity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModEntities {
	public static final EntityType<VanEntity> VAN = EntityType.Builder
			.create(VanEntity::new, SpawnGroup.MISC)
			.dimensions(2.0f, 1.9f)
			.maxTrackingRange(10)
			.build();

	public static final EntityType<DeliveryVanEntity> DELIVERY_VAN = EntityType.Builder
			.<DeliveryVanEntity>create(DeliveryVanEntity::new, SpawnGroup.MISC)
			.dimensions(2.0f, 1.9f)
			.maxTrackingRange(10)
			.build();

	public static final EntityType<DeliveryWorkerEntity> DELIVERY_WORKER = EntityType.Builder
			.create(DeliveryWorkerEntity::new, SpawnGroup.MISC)
			.dimensions(0.6f, 1.95f)
			.maxTrackingRange(10)
			.build();

	public static void register() {
		Registry.register(Registries.ENTITY_TYPE, AmazingMod.id("van"), VAN);
		Registry.register(Registries.ENTITY_TYPE, AmazingMod.id("delivery_van"), DELIVERY_VAN);
		Registry.register(Registries.ENTITY_TYPE, AmazingMod.id("delivery_worker"), DELIVERY_WORKER);

		FabricDefaultAttributeRegistry.register(DELIVERY_WORKER, DeliveryWorkerEntity.createWorkerAttributes());

		ModItems.workerSpawnEgg = (SpawnEggItem) Registry.register(Registries.ITEM,
				AmazingMod.id("delivery_worker_spawn_egg"),
				new SpawnEggItem(DELIVERY_WORKER, 0x1a3c8f, 0xff9900, new Item.Settings()));
	}
}
