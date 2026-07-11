package com.ellosan.amazing.client.render;

import com.ellosan.amazing.entity.VanEntity;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

/**
 * The iconic Amazing van: boxy cargo body, stubby cab, four spinning wheels
 * (the front pair steers). Front of the van faces -Z, ground at y=24.
 */
public class VanEntityModel extends EntityModel<VanEntity> {
	private final ModelPart root;
	private final ModelPart wheelFrontLeft;
	private final ModelPart wheelFrontRight;
	private final ModelPart wheelRearLeft;
	private final ModelPart wheelRearRight;

	private float wheelAngle;
	private float steerAngle;

	public VanEntityModel(ModelPart root) {
		this.root = root;
		this.wheelFrontLeft = root.getChild("wheel_front_left");
		this.wheelFrontRight = root.getChild("wheel_front_right");
		this.wheelRearLeft = root.getChild("wheel_rear_left");
		this.wheelRearRight = root.getChild("wheel_rear_right");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		// Tall cargo box at the back.
		root.addChild("cargo", ModelPartBuilder.create()
						.uv(0, 0)
						.cuboid(-12.0f, -28.0f, -2.0f, 24.0f, 24.0f, 16.0f),
				ModelTransform.pivot(0.0f, 24.0f, 0.0f));

		// Lower cab with the windshield at the front.
		root.addChild("cab", ModelPartBuilder.create()
						.uv(0, 40)
						.cuboid(-12.0f, -20.0f, -14.0f, 24.0f, 16.0f, 12.0f),
				ModelTransform.pivot(0.0f, 24.0f, 0.0f));

		// Front bumper.
		root.addChild("bumper", ModelPartBuilder.create()
						.uv(0, 68)
						.cuboid(-12.0f, -8.0f, -16.0f, 24.0f, 4.0f, 2.0f),
				ModelTransform.pivot(0.0f, 24.0f, 0.0f));

		ModelPartBuilder wheel = ModelPartBuilder.create()
				.uv(0, 80)
				.cuboid(-2.0f, -3.0f, -3.0f, 4.0f, 6.0f, 6.0f);

		root.addChild("wheel_front_left", wheel, ModelTransform.pivot(9.0f, 21.0f, -9.0f));
		root.addChild("wheel_front_right", wheel, ModelTransform.pivot(-9.0f, 21.0f, -9.0f));
		root.addChild("wheel_rear_left", wheel, ModelTransform.pivot(9.0f, 21.0f, 9.0f));
		root.addChild("wheel_rear_right", wheel, ModelTransform.pivot(-9.0f, 21.0f, 9.0f));

		return TexturedModelData.of(modelData, 128, 128);
	}

	/** Drives the wheel animation; called by the renderer with lerped values. */
	public void setWheelState(float wheelAngle, float steerAngle) {
		this.wheelAngle = wheelAngle;
		this.steerAngle = MathHelper.clamp(steerAngle, -0.6f, 0.6f);
	}

	@Override
	public void setAngles(VanEntity entity, float limbAngle, float limbDistance, float animationProgress,
			float headYaw, float headPitch) {
		this.wheelFrontLeft.pitch = this.wheelAngle;
		this.wheelFrontRight.pitch = this.wheelAngle;
		this.wheelRearLeft.pitch = this.wheelAngle;
		this.wheelRearRight.pitch = this.wheelAngle;

		this.wheelFrontLeft.yaw = this.steerAngle;
		this.wheelFrontRight.yaw = this.steerAngle;
		this.wheelRearLeft.yaw = 0.0f;
		this.wheelRearRight.yaw = 0.0f;
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
		this.root.render(matrices, vertices, light, overlay, color);
	}
}
