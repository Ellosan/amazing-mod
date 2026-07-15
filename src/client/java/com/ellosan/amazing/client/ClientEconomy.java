package com.ellosan.amazing.client;

import java.util.List;

/** Client-side cache of server-synced economy/order/quest data. */
public final class ClientEconomy {
	public static volatile int balance;
	public static volatile int primeDaysLeft;
	public static volatile List<String> orders = List.of();
	public static volatile String quest = "";

	private ClientEconomy() {
	}

	public static boolean hasPrime() {
		return primeDaysLeft > 0;
	}
}
