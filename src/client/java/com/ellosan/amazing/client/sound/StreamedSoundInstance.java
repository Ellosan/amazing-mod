package com.ellosan.amazing.client.sound;

import com.ellosan.amazing.AmazingMod;

import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.AudioStream;
import net.minecraft.client.sound.OggAudioStream;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundLoader;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.floatprovider.ConstantFloatProvider;
import net.minecraft.util.math.random.Random;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.util.concurrent.CompletableFuture;

/**
 * A sound instance backed by an internet OGG Vorbis stream instead of a
 * bundled asset. The sound set and audio stream are provided in code, so no
 * sounds.json entry or asset file is needed.
 */
public class StreamedSoundInstance extends AbstractSoundInstance {
	private final String url;

	public StreamedSoundInstance(String url) {
		super(AmazingMod.id("radio_stream"), SoundCategory.RECORDS, Random.create());
		this.url = url;
		this.repeat = false;
		this.attenuationType = AttenuationType.NONE;
		this.relative = true;
		this.volume = 1.0f;
	}

	@Override
	public WeightedSoundSet getSoundSet(SoundManager soundManager) {
		this.sound = new Sound(this.getId(),
				ConstantFloatProvider.create(1.0f), ConstantFloatProvider.create(1.0f),
				1, Sound.RegistrationType.FILE, true, false, 16);
		return new WeightedSoundSet(this.getId(), null);
	}

	@Override
	public CompletableFuture<AudioStream> getAudioStream(SoundLoader loader, Identifier id, boolean repeatInstantly) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				// Follow redirects manually so https -> http station hops work
				// (Java refuses cross-protocol redirects on its own).
				String target = this.url;
				InputStream stream = null;
				for (int hop = 0; hop < 5; hop++) {
					URLConnection connection = URI.create(target).toURL().openConnection();
					connection.setConnectTimeout(6000);
					connection.setReadTimeout(8000);
					connection.setRequestProperty("User-Agent", "AmazingRadio/2.0 (Minecraft mod)");
					connection.setRequestProperty("Icy-MetaData", "0");
					if (connection instanceof HttpURLConnection http) {
						http.setInstanceFollowRedirects(false);
						int code = http.getResponseCode();
						if (code >= 300 && code < 400 && http.getHeaderField("Location") != null) {
							target = http.getHeaderField("Location");
							http.disconnect();
							continue;
						}
					}
					stream = new BufferedInputStream(connection.getInputStream(), 64 * 1024);
					break;
				}
				if (stream == null) {
					throw new IOException("Too many redirects");
				}
				RadioPlayer.setStatus("♪ Streaming");
				return (AudioStream) new OggAudioStream(stream);
			} catch (IOException e) {
				RadioPlayer.setStatus("Stream failed: " + e.getMessage());
				throw new RuntimeException(e);
			}
		}, Util.getIoWorkerExecutor());
	}
}
