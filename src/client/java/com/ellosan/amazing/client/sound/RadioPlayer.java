package com.ellosan.amazing.client.sound;

import net.minecraft.client.MinecraftClient;

import org.jetbrains.annotations.Nullable;

/** Plays internet radio (OGG Vorbis streams) through Minecraft's sound engine. */
public final class RadioPlayer {
	@Nullable
	private static StreamedSoundInstance current;
	private static String status = "Radio off";

	private RadioPlayer() {
	}

	public static void play(String url) {
		stop();
		if (url == null || url.isBlank()) {
			status = "No station URL";
			return;
		}
		current = new StreamedSoundInstance(url.trim());
		MinecraftClient.getInstance().getSoundManager().play(current);
		status = "Tuning in...";
	}

	public static void stop() {
		if (current != null) {
			MinecraftClient.getInstance().getSoundManager().stop(current);
			current = null;
		}
		status = "Radio off";
	}

	public static boolean isPlaying() {
		return current != null && MinecraftClient.getInstance().getSoundManager().isPlaying(current);
	}

	public static void setStatus(String newStatus) {
		status = newStatus;
	}

	public static String status() {
		if (current != null && isPlaying()) {
			return "♪ On air";
		}
		return status;
	}
}
