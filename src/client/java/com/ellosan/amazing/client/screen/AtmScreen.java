package com.ellosan.amazing.client.screen;

import com.ellosan.amazing.client.ClientEconomy;
import com.ellosan.amazing.net.BankOpPayload;
import com.ellosan.amazing.net.RequestSyncPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** The MineBank ATM interface. */
public class AtmScreen extends Screen {
	private static final int PANEL_WIDTH = 200;
	private static final int PANEL_HEIGHT = 190;

	private int left;
	private int top;

	public AtmScreen() {
		super(Text.literal("MineBank ATM"));
	}

	@Override
	protected void init() {
		this.left = (this.width - PANEL_WIDTH) / 2;
		this.top = (this.height - PANEL_HEIGHT) / 2;

		int y = this.top + 48;
		int[] amounts = {10, 50, 100};
		for (int i = 0; i < amounts.length; i++) {
			final int amount = amounts[i];
			this.addDrawableChild(ButtonWidget.builder(Text.literal("Withdraw $" + amount),
							ignored -> op("withdraw", amount))
					.dimensions(this.left + 12 + i * 60, y, 56, 18).build());
		}

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Deposit all cash"),
						ignored -> op("deposit_cash", 0))
				.dimensions(this.left + 12, y + 24, PANEL_WIDTH - 24, 18).build());
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Sell emeralds ($5 each)"),
						ignored -> op("sell_emeralds", 0))
				.dimensions(this.left + 12, y + 46, PANEL_WIDTH - 24, 18).build());
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Subscribe Prime ($20/month)"),
						ignored -> op("prime", 0))
				.dimensions(this.left + 12, y + 68, PANEL_WIDTH - 24, 18).build());
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), ignored -> this.close())
				.dimensions(this.left + PANEL_WIDTH / 2 - 30, y + 96, 60, 18).build());
	}

	private static void op(String operation, int amount) {
		ClientPlayNetworking.send(new BankOpPayload(operation, amount, ""));
		ClientPlayNetworking.send(new RequestSyncPayload());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		context.drawCenteredTextWithShadow(this.textRenderer,
				Text.literal("MineBank™ ATM").formatted(Formatting.BOLD),
				this.width / 2, this.top + 12, 0xFF66CCFF);
		context.drawCenteredTextWithShadow(this.textRenderer,
				Text.literal("Balance: $" + ClientEconomy.balance
						+ (ClientEconomy.hasPrime() ? "  •  Prime ★ " + ClientEconomy.primeDaysLeft + "d" : "")),
				this.width / 2, this.top + 28, 0xFF55FF55);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);
		context.fill(this.left - 3, this.top - 3, this.left + PANEL_WIDTH + 3, this.top + PANEL_HEIGHT + 3, 0xFF0A2A4A);
		context.fill(this.left, this.top, this.left + PANEL_WIDTH, this.top + PANEL_HEIGHT, 0xFF10151C);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
