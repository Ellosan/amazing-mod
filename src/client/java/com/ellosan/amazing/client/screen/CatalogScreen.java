package com.ellosan.amazing.client.screen;

import com.ellosan.amazing.net.OrderPayload;
import com.ellosan.amazing.registry.ModItems;
import com.ellosan.amazing.shop.Product;
import com.ellosan.amazing.shop.ProductCatalog;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Amazing catalog. Browse departments, search, and order with one click.
 * Shift-click Buy to order a 4-pack. All purchases are validated server-side.
 */
public class CatalogScreen extends Screen {
	private static final int ROWS_PER_PAGE = 6;
	private static final int ROW_HEIGHT = 26;

	@Nullable
	private Product.Category category;
	private String query = "";
	private int page;

	private List<Product> filtered = List.of();
	private TextFieldWidget searchField;

	private int panelLeft;
	private int panelTop;
	private int panelWidth;
	private int listTop;

	public CatalogScreen() {
		super(Text.literal("Amazing Catalog"));
	}

	@Override
	protected void init() {
		this.panelWidth = Math.min(400, this.width - 16);
		this.panelLeft = (this.width - this.panelWidth) / 2;
		this.panelTop = 8;

		this.refilter();

		// --- Department tabs (two rows) ---
		int tabY = this.panelTop + 26;
		int tabWidth = (this.panelWidth - 8) / 5;
		List<@Nullable Product.Category> tabs = new ArrayList<>();
		tabs.add(null); // "All"
		tabs.addAll(List.of(Product.Category.values()));
		for (int i = 0; i < tabs.size(); i++) {
			final Product.Category tabCategory = tabs.get(i);
			int col = i % 5;
			int row = i / 5;
			String label = tabCategory == null ? "All" : shortName(tabCategory);
			ButtonWidget button = ButtonWidget.builder(Text.literal(label), ignored -> {
						this.category = tabCategory;
						this.page = 0;
						this.clearAndInit();
					})
					.dimensions(this.panelLeft + 4 + col * tabWidth, tabY + row * 18, tabWidth - 2, 16)
					.build();
			button.active = this.category != tabCategory;
			this.addDrawableChild(button);
		}

		// --- Search box ---
		int searchY = tabY + 40;
		this.searchField = new TextFieldWidget(this.textRenderer,
				this.panelLeft + 4, searchY, this.panelWidth - 8, 14, Text.literal("Search"));
		this.searchField.setText(this.query);
		this.searchField.setChangedListener(text -> {
			this.query = text;
			this.page = 0;
			this.refilter();
			this.rebuildRowButtons();
		});
		this.addDrawableChild(this.searchField);

		this.listTop = searchY + 20;

		// --- Pagination ---
		int pageY = this.listTop + ROWS_PER_PAGE * ROW_HEIGHT + 4;
		this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), ignored -> {
					if (this.page > 0) {
						this.page--;
						this.clearAndInit();
					}
				})
				.dimensions(this.panelLeft + 4, pageY, 20, 16).build());
		this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), ignored -> {
					if ((this.page + 1) * ROWS_PER_PAGE < this.filtered.size()) {
						this.page++;
						this.clearAndInit();
					}
				})
				.dimensions(this.panelLeft + this.panelWidth - 24, pageY, 20, 16).build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), ignored -> this.close())
				.dimensions(this.panelLeft + this.panelWidth / 2 - 30, pageY + 18, 60, 16).build());

		this.rebuildRowButtons();
	}

	private final List<ButtonWidget> rowButtons = new ArrayList<>();

	private void rebuildRowButtons() {
		for (ButtonWidget button : this.rowButtons) {
			this.remove(button);
		}
		this.rowButtons.clear();

		List<Product> visible = this.visibleProducts();
		for (int i = 0; i < visible.size(); i++) {
			final Product product = visible.get(i);
			int y = this.listTop + i * ROW_HEIGHT;
			ButtonWidget buy = ButtonWidget.builder(Text.literal("Buy"), ignored -> {
						int quantity = Screen.hasShiftDown() ? 4 : 1;
						ClientPlayNetworking.send(new OrderPayload(product.id(), quantity));
						this.close();
					})
					.dimensions(this.panelLeft + this.panelWidth - 40, y + 3, 36, 18)
					.build();
			buy.active = this.canAfford(product) && this.hasPrimeAccess(product);
			this.rowButtons.add(buy);
			this.addDrawableChild(buy);
		}
	}

	private void refilter() {
		String needle = this.query.toLowerCase(Locale.ROOT).trim();
		List<Product> result = new ArrayList<>();
		for (Product product : ProductCatalog.all()) {
			if (this.category != null && product.category() != this.category) {
				continue;
			}
			if (!needle.isEmpty() && !product.name().toLowerCase(Locale.ROOT).contains(needle)) {
				continue;
			}
			result.add(product);
		}
		this.filtered = result;
		int maxPage = Math.max(0, (this.filtered.size() - 1) / ROWS_PER_PAGE);
		this.page = Math.min(this.page, maxPage);
	}

	private List<Product> visibleProducts() {
		int from = this.page * ROWS_PER_PAGE;
		int to = Math.min(this.filtered.size(), from + ROWS_PER_PAGE);
		return from >= to ? List.of() : this.filtered.subList(from, to);
	}

	private int emeraldBalance() {
		if (this.client == null || this.client.player == null) {
			return 0;
		}
		int total = 0;
		var inventory = this.client.player.getInventory();
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (stack.isOf(Items.EMERALD)) {
				total += stack.getCount();
			}
		}
		return total;
	}

	private boolean isPrimeMember() {
		if (this.client == null || this.client.player == null) {
			return false;
		}
		if (this.client.player.getAbilities().creativeMode) {
			return true;
		}
		var inventory = this.client.player.getInventory();
		for (int i = 0; i < inventory.size(); i++) {
			if (inventory.getStack(i).isOf(ModItems.PRIME_CARD)) {
				return true;
			}
		}
		return false;
	}

	private boolean canAfford(Product product) {
		return (this.client != null && this.client.player != null
				&& this.client.player.getAbilities().creativeMode)
				|| this.emeraldBalance() >= product.price();
	}

	private boolean hasPrimeAccess(Product product) {
		return !product.prime() || this.isPrimeMember();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		// Header.
		context.drawText(this.textRenderer, Text.literal("amazing").formatted(Formatting.BOLD),
				this.panelLeft + 6, this.panelTop + 6, 0xFFFFFFFF, true);
		int logoWidth = this.textRenderer.getWidth(Text.literal("amazing").formatted(Formatting.BOLD));
		context.drawText(this.textRenderer, Text.literal("⌣").formatted(Formatting.GOLD),
				this.panelLeft + 6 + logoWidth / 3, this.panelTop + 12, 0xFFFF9900, false);
		context.drawText(this.textRenderer,
				Text.literal("Earth's Blockiest Store").formatted(Formatting.ITALIC),
				this.panelLeft + 6 + logoWidth + 8, this.panelTop + 8, 0xFFAAAAAA, false);

		boolean prime = this.isPrimeMember();
		String wallet = "Wallet: " + this.emeraldBalance() + " emeralds" + (prime ? "  •  PRIME ✔" : "");
		context.drawText(this.textRenderer, Text.literal(wallet),
				this.panelLeft + this.panelWidth - this.textRenderer.getWidth(wallet) - 6,
				this.panelTop + 8, prime ? 0xFFDD88FF : 0xFF55FF55, true);

		// Product rows.
		List<Product> visible = this.visibleProducts();
		for (int i = 0; i < visible.size(); i++) {
			Product product = visible.get(i);
			int y = this.listTop + i * ROW_HEIGHT;

			context.fill(this.panelLeft + 2, y, this.panelLeft + this.panelWidth - 2, y + ROW_HEIGHT - 2,
					i % 2 == 0 ? 0x40000000 : 0x30000000);

			context.drawItem(product.createStack(), this.panelLeft + 6, y + 4);

			String name = product.name();
			if (product.count() > 1) {
				name = name + " (x" + product.count() + ")";
			}
			context.drawText(this.textRenderer, Text.literal(name),
					this.panelLeft + 28, y + 4, 0xFFFFFFFF, false);

			String priceLine = product.price() + " emeralds"
					+ (product.prime() ? "  [PRIME]" : "");
			int priceColor = this.hasPrimeAccess(product)
					? (this.canAfford(product) ? 0xFF55FF55 : 0xFFFF5555)
					: 0xFFDD88FF;
			context.drawText(this.textRenderer, Text.literal(priceLine),
					this.panelLeft + 28, y + 14, priceColor, false);
		}

		if (visible.isEmpty()) {
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.literal("No results. Even Amazing has limits."),
					this.width / 2, this.listTop + 40, 0xFFAAAAAA);
		}

		// Footer.
		int pageY = this.listTop + ROWS_PER_PAGE * ROW_HEIGHT + 8;
		int totalPages = Math.max(1, (this.filtered.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
		context.drawCenteredTextWithShadow(this.textRenderer,
				Text.literal("Page " + (this.page + 1) + "/" + totalPages
						+ "  —  Shift-click Buy for a 4-pack"),
				this.width / 2, pageY, 0xFF888888);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);
		context.fillGradient(this.panelLeft, 0, this.panelLeft + this.panelWidth, this.height,
				0xC0101820, 0xC0101014);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private static String shortName(Product.Category category) {
		return switch (category) {
			case TOOLS -> "Tools";
			case COMBAT -> "Combat";
			case FOOD -> "Grocery";
			case BLOCKS -> "Home";
			case REDSTONE -> "Tech";
			case FARMING -> "Garden";
			case BREWING -> "Alchemy";
			case RARE -> "Prime";
		};
	}
}
