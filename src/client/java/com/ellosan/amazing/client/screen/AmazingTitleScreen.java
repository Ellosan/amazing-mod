package com.ellosan.amazing.client.screen;

import com.ellosan.amazing.AmazingMod;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;

/**
 * The Amazing™ title screen: a sunny sky, rolling clouds, and an Amazing van
 * doing its rounds along the bottom of the screen. Replaces the vanilla title
 * screen via {@link com.ellosan.amazing.client.mixin.MinecraftClientMixin}.
 */
public class AmazingTitleScreen extends Screen {
	private static final Identifier LOGO = AmazingMod.id("textures/gui/amazing_logo.png");
	private static final Identifier VAN = AmazingMod.id("textures/gui/title_van.png");

	/** One-shot escape hatch so the "Classic Menu" button can reach vanilla. */
	public static boolean bypassOnce = false;

	private static final String[] SPLASHES = {
			"Free shipping over 64 emeralds!",
			"Now delivering to the Nether*",
			"Earth's Blockiest Store!",
			"Try Amazing Prime today!",
			"1-emerald deals every day!",
			"Vans! Vans! Vans!",
			"Your villagers already shop here!",
			"Same-day delivery, most biomes!",
			"HONK if you love packages!",
			"*Nether delivery not actually available",
	};

	private final String splash;
	private float ticks;

	public AmazingTitleScreen() {
		super(Text.literal("Amazing"));
		this.splash = SPLASHES[Random.create().nextInt(SPLASHES.length)];
	}

	@Override
	protected void init() {
		int buttonWidth = 200;
		int x = this.width / 2 - buttonWidth / 2;
		int y = this.height / 4 + 58;

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Singleplayer"),
						ignored -> this.client.setScreen(new SelectWorldScreen(this)))
				.dimensions(x, y, buttonWidth, 20).build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Multiplayer"),
						ignored -> this.client.setScreen(new MultiplayerScreen(this)))
				.dimensions(x, y + 24, buttonWidth, 20).build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Options..."),
						ignored -> this.client.setScreen(new OptionsScreen(this, this.client.options)))
				.dimensions(x, y + 48, buttonWidth / 2 - 2, 20).build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Quit"),
						ignored -> this.client.scheduleStop())
				.dimensions(x + buttonWidth / 2 + 2, y + 48, buttonWidth / 2 - 2, 20).build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Classic Menu"),
						ignored -> {
							bypassOnce = true;
							this.client.setScreen(new TitleScreen());
						})
				.dimensions(this.width - 82, this.height - 24, 78, 20).build());
	}

	@Override
	public void tick() {
		super.tick();
		this.ticks++;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		float time = this.ticks + delta;
		this.renderScenery(context, time);

		super.render(context, mouseX, mouseY, delta);

		// Logo (312x78 on screen, 512x128 texture).
		int logoWidth = 312;
		int logoHeight = 78;
		int logoX = this.width / 2 - logoWidth / 2;
		int logoY = Math.max(10, this.height / 4 - 48);
		context.drawTexture(LOGO, logoX, logoY, logoWidth, logoHeight, 0.0f, 0.0f, 512, 128, 512, 128);

		// Pulsing yellow splash, vanilla style.
		float pulse = 1.7f - MathHelper.abs(MathHelper.sin(time / 8.0f)) * 0.1f;
		context.getMatrices().push();
		context.getMatrices().translate(this.width / 2.0f + logoWidth / 2.0f - 30, logoY + logoHeight - 14, 0.0f);
		context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0f));
		context.getMatrices().scale(pulse, pulse, pulse);
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(this.splash), 0, 0, 0xFFFFFF00);
		context.getMatrices().pop();

		context.drawTextWithShadow(this.textRenderer,
				Text.literal("Amazing Edition v1.0 (Minecraft 1.21.1)"), 2, this.height - 10, 0xFFDDDDDD);
		context.drawTextWithShadow(this.textRenderer, Text.literal("Not affiliated with any rainforest"),
				2, this.height - 20, 0xFF888888);
	}

	private void renderScenery(DrawContext context, float time) {
		// Sky.
		context.fillGradient(0, 0, this.width, this.height, 0xFF7EC8F2, 0xFFCDEBFA);

		// Drifting clouds.
		for (int i = 0; i < 5; i++) {
			int cloudWidth = 60 + (i * 17) % 40;
			float speed = 0.12f + i * 0.05f;
			int cx = (int) ((time * speed + i * 197) % (this.width + cloudWidth)) - cloudWidth;
			int cy = 18 + (i * 37) % 60;
			context.fill(cx, cy, cx + cloudWidth, cy + 10, 0xE0FFFFFF);
			context.fill(cx + 10, cy - 6, cx + cloudWidth - 14, cy, 0xE0FFFFFF);
		}

		// Grass and road.
		int roadTop = this.height - 46;
		context.fill(0, roadTop - 8, this.width, roadTop, 0xFF6DA83A);
		context.fill(0, roadTop, this.width, this.height, 0xFF6E6E6E);
		// Dashed center line.
		for (int x = -(int) (time * 2.5f) % 24; x < this.width; x += 24) {
			context.fill(x, roadTop + 18, x + 12, roadTop + 21, 0xFFF2D22E);
		}

		// The hero of the story, driving by (96x48 on screen).
		int vanWidth = 96;
		int vanHeight = 48;
		int vanX = (int) ((time * 1.8f) % (this.width + vanWidth * 2)) - vanWidth;
		int bob = (int) (MathHelper.abs(MathHelper.sin(time / 3.0f)) * 2.0f);
		context.drawTexture(VAN, vanX, roadTop - vanHeight + 14 - bob, vanWidth, vanHeight,
				0.0f, 0.0f, 128, 64, 128, 64);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		// Scenery is the background; skip vanilla panorama/blur.
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
}
