package com.ellosan.amazing.client.render;

import com.ellosan.amazing.AmazingMod;
import com.ellosan.amazing.client.AmazingModClient;
import com.ellosan.amazing.entity.DeliveryWorkerEntity;

import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.util.Identifier;

public class DeliveryWorkerRenderer
		extends BipedEntityRenderer<DeliveryWorkerEntity, BipedEntityModel<DeliveryWorkerEntity>> {
	private static final Identifier TEXTURE = AmazingMod.id("textures/entity/delivery_worker.png");

	public DeliveryWorkerRenderer(EntityRendererFactory.Context context) {
		super(context, new BipedEntityModel<>(context.getPart(AmazingModClient.WORKER_LAYER)), 0.5f);
	}

	@Override
	public Identifier getTexture(DeliveryWorkerEntity entity) {
		return TEXTURE;
	}
}
