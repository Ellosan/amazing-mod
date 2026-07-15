package com.ellosan.amazing.client.render;

import com.ellosan.amazing.AmazingMod;
import com.ellosan.amazing.client.AmazingModClient;
import com.ellosan.amazing.entity.CitizenEntity;

import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.util.Identifier;

public class CitizenRenderer extends BipedEntityRenderer<CitizenEntity, BipedEntityModel<CitizenEntity>> {
	private static final Identifier[] TEXTURES = new Identifier[CitizenEntity.VARIANTS];

	static {
		for (int i = 0; i < CitizenEntity.VARIANTS; i++) {
			TEXTURES[i] = AmazingMod.id("textures/entity/citizen_" + i + ".png");
		}
	}

	public CitizenRenderer(EntityRendererFactory.Context context) {
		super(context, new BipedEntityModel<>(context.getPart(AmazingModClient.WORKER_LAYER)), 0.5f);
	}

	@Override
	public Identifier getTexture(CitizenEntity entity) {
		return TEXTURES[entity.getVariant()];
	}
}
