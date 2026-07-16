package com.ellosan.amazing.client.screen;

import com.ellosan.amazing.client.ClientEconomy;
import com.ellosan.amazing.client.sound.RadioPlayer;
import com.ellosan.amazing.net.BankOpPayload;
import com.ellosan.amazing.net.PhoneChatPayload;
import com.ellosan.amazing.net.RequestSyncPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The AmazingPhone X. One screen, many apps: Amazing (orders), MineBank
 * (e-banking + Prime), GPS, Quests, Radio (internet streams), and Calls.
 */
public class PhoneScreen extends Screen {
	private enum App {
		HOME("AmazingOS"),
		AMAZING("Amazing"),
		BANK("MineBank"),
		GPS("GPS"),
		QUESTS("Quests"),
		RADIO("Radio"),
		CHAT("Chat"),
		CALL("Phone");

		final String title;

		App(String title) {
			this.title = title;
		}
	}

	private static final String[][] STATIONS = {
			{"Chinese Music World", "https://radio.chinesemusicworld.com/chinesemusic.ogg"},
			{"Dance Wave!", "https://dancewave.online/dance.ogg"},
			{"Radio 1 Rock (BG)", "http://stream.metacast.eu/radio1rock.ogg"},
	};

	@Nullable
	private static BlockPos waypoint;

	private App app = App.HOME;
	private int left;
	private int top;
	private static final int PHONE_WIDTH = 180;
	private static final int PHONE_HEIGHT = 240;

	private TextFieldWidget urlField;
	private TextFieldWidget transferField;
	private TextFieldWidget chatField;
	private TextFieldWidget chatTarget;
	private String chatStatus = "";
	private String callLine = "";

	public PhoneScreen() {
		super(Text.literal("AmazingPhone"));
		ClientPlayNetworking.send(new RequestSyncPayload());
	}

	@Override
	protected void init() {
		this.left = (this.width - PHONE_WIDTH) / 2;
		this.top = (this.height - PHONE_HEIGHT) / 2;

		switch (this.app) {
			case HOME -> this.initHome();
			case AMAZING -> this.initAmazing();
			case BANK -> this.initBank();
			case GPS -> this.initGps();
			case QUESTS -> this.initQuests();
			case RADIO -> this.initRadio();
			case CHAT -> this.initChat();
			case CALL -> this.initCall();
		}

		if (this.app != App.HOME) {
			this.addDrawableChild(ButtonWidget.builder(Text.literal("< Home"), ignored -> this.open(App.HOME))
					.dimensions(this.left + 10, this.top + PHONE_HEIGHT - 26, 50, 16).build());
		}
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Lock"), ignored -> this.close())
				.dimensions(this.left + PHONE_WIDTH - 50, this.top + PHONE_HEIGHT - 26, 40, 16).build());
	}

	private void open(App newApp) {
		this.app = newApp;
		this.callLine = "";
		this.clearAndInit();
	}

	private void initHome() {
		App[] apps = {App.AMAZING, App.BANK, App.GPS, App.QUESTS, App.RADIO, App.CHAT, App.CALL};
		for (int i = 0; i < apps.length; i++) {
			final App target = apps[i];
			int col = i % 2;
			int row = i / 2;
			this.addDrawableChild(ButtonWidget.builder(Text.literal(target.title), ignored -> this.open(target))
					.dimensions(this.left + 14 + col * 78, this.top + 52 + row * 30, 74, 24).build());
		}
	}

	private void initAmazing() {
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Open Catalog"),
						ignored -> this.client.setScreen(new CatalogScreen()))
				.dimensions(this.left + 14, this.top + PHONE_HEIGHT - 56, PHONE_WIDTH - 28, 18).build());
	}

	private void initBank() {
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Prime $" + 20 + "/mo"),
						ignored -> bankOp("prime", 0, ""))
				.dimensions(this.left + 14, this.top + 96, PHONE_WIDTH - 28, 18).build());
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Deposit cash"),
						ignored -> bankOp("deposit_cash", 0, ""))
				.dimensions(this.left + 14, this.top + 118, 74, 18).build());
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Sell emeralds"),
						ignored -> bankOp("sell_emeralds", 0, ""))
				.dimensions(this.left + 92, this.top + 118, 74, 18).build());

		this.transferField = new TextFieldWidget(this.textRenderer,
				this.left + 14, this.top + 152, PHONE_WIDTH - 28, 14, Text.literal("player name"));
		this.transferField.setPlaceholder(Text.literal("player name...").formatted(Formatting.DARK_GRAY));
		this.addDrawableChild(this.transferField);

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Send $10"),
						ignored -> bankOp("transfer", 10, this.transferField.getText()))
				.dimensions(this.left + 14, this.top + 170, 74, 18).build());
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Send $50"),
						ignored -> bankOp("transfer", 50, this.transferField.getText()))
				.dimensions(this.left + 92, this.top + 170, 74, 18).build());
	}

	private void initGps() {
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Set waypoint here"), ignored -> {
					if (this.client.player != null) {
						waypoint = this.client.player.getBlockPos();
					}
				})
				.dimensions(this.left + 14, this.top + PHONE_HEIGHT - 56, PHONE_WIDTH - 28, 18).build());
	}

	private void initQuests() {
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Abandon quest"), ignored -> {
					if (this.client.player != null) {
						this.client.player.networkHandler.sendChatCommand("amazing quest abandon");
						ClientPlayNetworking.send(new RequestSyncPayload());
					}
				})
				.dimensions(this.left + 14, this.top + PHONE_HEIGHT - 56, PHONE_WIDTH - 28, 18).build());
	}

	private void initRadio() {
		int y = this.top + 52;
		for (String[] station : STATIONS) {
			final String url = station[1];
			this.addDrawableChild(ButtonWidget.builder(Text.literal("▶ " + station[0]),
							ignored -> RadioPlayer.play(url))
					.dimensions(this.left + 14, y, PHONE_WIDTH - 28, 18).build());
			y += 22;
		}

		this.urlField = new TextFieldWidget(this.textRenderer,
				this.left + 14, y + 4, PHONE_WIDTH - 28, 14, Text.literal("stream url"));
		this.urlField.setMaxLength(256);
		this.urlField.setPlaceholder(Text.literal("custom .ogg stream url...").formatted(Formatting.DARK_GRAY));
		this.addDrawableChild(this.urlField);

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Play URL"),
						ignored -> RadioPlayer.play(this.urlField.getText()))
				.dimensions(this.left + 14, y + 22, 74, 18).build());
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Stop"),
						ignored -> RadioPlayer.stop())
				.dimensions(this.left + 92, y + 22, 74, 18).build());
	}

	private void initChat() {
		this.chatTarget = new TextFieldWidget(this.textRenderer,
				this.left + 14, this.top + 40, PHONE_WIDTH - 28, 14, Text.literal("to"));
		this.chatTarget.setPlaceholder(Text.literal("to: everyone (or player name)").formatted(Formatting.DARK_GRAY));
		this.addDrawableChild(this.chatTarget);

		this.chatField = new TextFieldWidget(this.textRenderer,
				this.left + 14, this.top + 60, PHONE_WIDTH - 28, 14, Text.literal("message"));
		this.chatField.setMaxLength(256);
		this.chatField.setPlaceholder(Text.literal("message...").formatted(Formatting.DARK_GRAY));
		this.addDrawableChild(this.chatField);

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Send"), ignored -> {
					String message = this.chatField.getText().strip();
					if (!message.isEmpty()) {
						ClientPlayNetworking.send(new PhoneChatPayload(this.chatTarget.getText().strip(), message));
						this.chatField.setText("");
						this.chatStatus = "Sent!";
					}
				})
				.dimensions(this.left + 14, this.top + 80, PHONE_WIDTH - 28, 18).build());
	}

	private void initCall() {
		String[][] contacts = {
				{"Mom", "Mom: \"Are you eating enough golden carrots, sweetie?\""},
				{"Amazing Support", "Support: \"Have you tried turning the package off and on again?\""},
				{"MineBank Fraud Dept", "MineBank: \"Did you REALLY order 8 anvils? ...Okay, just checking.\""},
				{"Unknown Number", "???: \"Your van's extended warranty has expired.\""},
		};
		int y = this.top + 52;
		for (String[] contact : contacts) {
			final String line = contact[1];
			this.addDrawableChild(ButtonWidget.builder(Text.literal("📞 " + contact[0]), ignored -> {
						this.callLine = line;
					})
					.dimensions(this.left + 14, y, PHONE_WIDTH - 28, 18).build());
			y += 22;
		}
	}

	private static void bankOp(String op, int amount, String target) {
		ClientPlayNetworking.send(new BankOpPayload(op, amount, target == null ? "" : target));
		ClientPlayNetworking.send(new RequestSyncPayload());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		int textLeft = this.left + 14;
		// Status bar.
		context.drawText(this.textRenderer, Text.literal(this.app.title).formatted(Formatting.BOLD),
				textLeft, this.top + 12, 0xFFFFFFFF, false);
		long timeOfDay = this.client != null && this.client.world != null
				? this.client.world.getTimeOfDay() % 24000 : 0;
		String clock = String.format("%02d:%02d", (int) ((timeOfDay / 1000 + 6) % 24),
				(int) (timeOfDay % 1000 * 60 / 1000));
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(clock),
				this.left + PHONE_WIDTH / 2, this.top + 12, 0xFFCCCCCC);
		String balanceText = "$" + ClientEconomy.balance + (ClientEconomy.hasPrime() ? " ★" : "");
		context.drawText(this.textRenderer, Text.literal(balanceText),
				this.left + PHONE_WIDTH - 14 - this.textRenderer.getWidth(balanceText),
				this.top + 12, 0xFF55FF55, false);
		context.fill(textLeft, this.top + 24, this.left + PHONE_WIDTH - 14, this.top + 25, 0x40FFFFFF);

		switch (this.app) {
			case HOME -> context.drawCenteredTextWithShadow(this.textRenderer,
					Text.literal("~ blocky. fast. amazing. ~").formatted(Formatting.GRAY),
					this.left + PHONE_WIDTH / 2, this.top + 34, 0xFFAAAAAA);
			case AMAZING -> this.renderAmazing(context, textLeft);
			case BANK -> this.renderBank(context, textLeft);
			case GPS -> this.renderGps(context, textLeft);
			case QUESTS -> this.renderText(context, textLeft, this.top + 40,
					ClientEconomy.quest.isEmpty() ? "No quest data. Poke a courier!" : ClientEconomy.quest);
			case RADIO -> context.drawText(this.textRenderer, Text.literal(RadioPlayer.status()),
					textLeft, this.top + 36, 0xFFFFCC44, false);
			case CHAT -> {
				this.renderText(context, textLeft, this.top + 106,
						this.chatStatus.isEmpty() ? "Messages appear in game chat." : this.chatStatus);
				this.renderText(context, textLeft, this.top + 120,
						"Voice chat? Install the Simple Voice Chat mod alongside Amazing.");
			}
			case CALL -> this.renderText(context, textLeft, this.top + 150,
					this.callLine.isEmpty() ? "Who you gonna call?" : this.callLine);
		}
	}

	private void renderAmazing(DrawContext context, int textLeft) {
		context.drawText(this.textRenderer, Text.literal("Order tracking").formatted(Formatting.GOLD),
				textLeft, this.top + 34, 0xFFFF9900, false);
		List<String> orders = ClientEconomy.orders;
		if (orders.isEmpty()) {
			context.drawText(this.textRenderer, Text.literal("No deliveries on the way."),
					textLeft, this.top + 48, 0xFFAAAAAA, false);
		} else {
			int y = this.top + 48;
			for (int i = 0; i < Math.min(orders.size(), 9); i++) {
				this.renderText(context, textLeft, y, "• " + orders.get(i));
				y += 14;
			}
		}
	}

	private void renderBank(DrawContext context, int textLeft) {
		context.drawText(this.textRenderer, Text.literal("Balance: $" + ClientEconomy.balance),
				textLeft, this.top + 38, 0xFF55FF55, false);
		String prime = ClientEconomy.hasPrime()
				? "Prime: active, " + ClientEconomy.primeDaysLeft + " days left"
				: "Prime: not subscribed";
		context.drawText(this.textRenderer, Text.literal(prime),
				textLeft, this.top + 52, ClientEconomy.hasPrime() ? 0xFFDD88FF : 0xFFAAAAAA, false);
		context.drawText(this.textRenderer, Text.literal("Withdraw cash at any ATM"),
				textLeft, this.top + 68, 0xFF888888, false);
		context.drawText(this.textRenderer, Text.literal("Transfer:"),
				textLeft, this.top + 142, 0xFFAAAAAA, false);
	}

	private void renderGps(DrawContext context, int textLeft) {
		if (this.client == null || this.client.player == null) {
			return;
		}
		BlockPos pos = this.client.player.getBlockPos();
		int y = this.top + 36;
		this.renderText(context, textLeft, y, "X: " + pos.getX() + "  Y: " + pos.getY() + "  Z: " + pos.getZ());
		String[] compass = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
		int heading = MathHelper.floorMod(Math.round(this.client.player.getYaw() / 45.0f), 8);
		this.renderText(context, textLeft, y + 14, "Facing: " + compass[heading]);
		long time = this.client.world != null ? this.client.world.getTimeOfDay() % 24000 : 0;
		this.renderText(context, textLeft, y + 28, "Time: " + String.format("%02d:%02d",
				(int) ((time / 1000 + 6) % 24), (int) (time % 1000 * 60 / 1000)));

		if (waypoint != null) {
			double dx = waypoint.getX() - pos.getX();
			double dz = waypoint.getZ() - pos.getZ();
			int distance = (int) Math.sqrt(dx * dx + dz * dz);
			float desired = (float) (MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
			float relative = MathHelper.wrapDegrees(desired - this.client.player.getYaw());
			String arrow = relative > 30 ? "→" : relative < -30 ? "←" : "↑";
			this.renderText(context, textLeft, y + 48, "Waypoint: " + distance + "m " + arrow);
		} else {
			this.renderText(context, textLeft, y + 48, "No waypoint set");
		}
	}

	private void renderText(DrawContext context, int x, int y, String message) {
		// Wrap long lines to the phone width.
		List<net.minecraft.text.OrderedText> lines = this.textRenderer.wrapLines(
				net.minecraft.text.StringVisitable.plain(message), PHONE_WIDTH - 28);
		for (net.minecraft.text.OrderedText line : lines) {
			context.drawText(this.textRenderer, line, x, y, 0xFFFFFFFF, false);
			y += 10;
		}
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);
		// Phone body + screen.
		context.fill(this.left - 4, this.top - 4, this.left + PHONE_WIDTH + 4, this.top + PHONE_HEIGHT + 4, 0xFF15151A);
		context.fill(this.left, this.top, this.left + PHONE_WIDTH, this.top + PHONE_HEIGHT, 0xFF23232E);
		context.fill(this.left + PHONE_WIDTH / 2 - 12, this.top - 2, this.left + PHONE_WIDTH / 2 + 12, this.top + 2,
				0xFF15151A); // notch, obviously
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
