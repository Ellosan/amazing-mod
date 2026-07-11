package com.ellosan.amazing.client.render;

import com.ellosan.amazing.client.AmazingModClient;
import com.ellosan.amazing.entity.VanEntity;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class VanEntityRenderer extends EntityRenderer<VanEntity> {
	private final VanEntityModel model;
	private final Identifier texture;

	public VanEntityRenderer(EntityRendererFactory.Context context, Identifier texture) {
		super(context);
		this.model = new VanEntityModel(context.getPart(AmazingModClient.VAN_LAYER));
		this.texture = texture;
		this.shadowRadius = 1.1f;
	}

	@Override
	public void render(VanEntity entity, float yaw, float tickDelta, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light) {
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - yaw));
		// Entity models are built y-down; flip into world space like living renderers do.
		matrices.scale(-1.0f, -1.0f, 1.0f);
		matrices.translate(0.0f, -1.501f, 0.0f);

		float wheelAngle = MathHelper.lerp(tickDelta, entity.prevWheelAngle, entity.wheelAngle);
		float steer = MathHelper.clamp(entity.yawDelta * 0.09f, -0.55f, 0.55f);
		this.model.setWheelState(wheelAngle, steer);
		this.model.setAngles(entity, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);

		VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(this.texture));
		this.model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, -1);

		matrices.pop();
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}

	@Override
	public Identifier getTexture(VanEntity entity) {
		return this.texture;
	}
}
