package com.ellosan.amazing.delivery;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Saved Amazing data: pending orders and per-player quest progress.
 * Stored with the overworld so it works identically in singleplayer
 * and on dedicated servers.
 */
public class AmazingWorldState extends PersistentState {
	private static final String STORAGE_KEY = "amazing_state";

	public static final PersistentState.Type<AmazingWorldState> TYPE =
			new PersistentState.Type<>(AmazingWorldState::new, AmazingWorldState::fromNbt, null);

	public final List<PendingOrder> orders = new ArrayList<>();
	public final Map<UUID, QuestData> quests = new HashMap<>();

	public static AmazingWorldState get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, STORAGE_KEY);
	}

	public QuestData questData(UUID playerUuid) {
		return this.quests.computeIfAbsent(playerUuid, uuid -> new QuestData());
	}

	public static AmazingWorldState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		AmazingWorldState state = new AmazingWorldState();

		NbtList orderList = nbt.getList("Orders", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < orderList.size(); i++) {
			state.orders.add(PendingOrder.fromNbt(orderList.getCompound(i)));
		}

		NbtList questList = nbt.getList("Quests", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < questList.size(); i++) {
			NbtCompound entry = questList.getCompound(i);
			state.quests.put(entry.getUuid("Player"), QuestData.fromNbt(entry));
		}

		return state;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		NbtList orderList = new NbtList();
		for (PendingOrder order : this.orders) {
			orderList.add(order.toNbt());
		}
		nbt.put("Orders", orderList);

		NbtList questList = new NbtList();
		for (Map.Entry<UUID, QuestData> entry : this.quests.entrySet()) {
			NbtCompound questNbt = entry.getValue().toNbt();
			questNbt.putUuid("Player", entry.getKey());
			questList.add(questNbt);
		}
		nbt.put("Quests", questList);

		return nbt;
	}

	/** An order that has been paid for and is waiting for its van. */
	public static class PendingOrder {
		public UUID customer;
		public String productId;
		public int quantity;
		public int ticksLeft;

		public PendingOrder(UUID customer, String productId, int quantity, int ticksLeft) {
			this.customer = customer;
			this.productId = productId;
			this.quantity = quantity;
			this.ticksLeft = ticksLeft;
		}

		public NbtCompound toNbt() {
			NbtCompound nbt = new NbtCompound();
			nbt.putUuid("Customer", this.customer);
			nbt.putString("Product", this.productId);
			nbt.putInt("Quantity", this.quantity);
			nbt.putInt("TicksLeft", this.ticksLeft);
			return nbt;
		}

		public static PendingOrder fromNbt(NbtCompound nbt) {
			return new PendingOrder(nbt.getUuid("Customer"), nbt.getString("Product"),
					Math.max(1, nbt.getInt("Quantity")), nbt.getInt("TicksLeft"));
		}
	}

	/** Per-player quest progress. */
	public static class QuestData {
		public String activeType = "";
		public String questId = "";
		public String targetItemId = "";
		public int targetCount;
		public int reward;
		public int questsCompleted;

		public boolean hasActiveQuest() {
			return !this.activeType.isEmpty();
		}

		public void clearActive() {
			this.activeType = "";
			this.questId = "";
			this.targetItemId = "";
			this.targetCount = 0;
			this.reward = 0;
		}

		public NbtCompound toNbt() {
			NbtCompound nbt = new NbtCompound();
			nbt.putString("ActiveType", this.activeType);
			nbt.putString("QuestId", this.questId);
			nbt.putString("TargetItem", this.targetItemId);
			nbt.putInt("TargetCount", this.targetCount);
			nbt.putInt("Reward", this.reward);
			nbt.putInt("Completed", this.questsCompleted);
			return nbt;
		}

		public static QuestData fromNbt(NbtCompound nbt) {
			QuestData data = new QuestData();
			data.activeType = nbt.getString("ActiveType");
			data.questId = nbt.getString("QuestId");
			data.targetItemId = nbt.getString("TargetItem");
			data.targetCount = nbt.getInt("TargetCount");
			data.reward = nbt.getInt("Reward");
			data.questsCompleted = nbt.getInt("Completed");
			return data;
		}
	}
}
