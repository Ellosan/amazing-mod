package com.ellosan.amazing.client.mixin;

import com.ellosan.amazing.client.screen.AmazingTitleScreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every time the game would show the vanilla title screen, show the Amazing
 * title screen instead. The "Classic Menu" button sets a one-shot bypass so
 * players can still reach the vanilla menu.
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	private void amazing$swapTitleScreen(Screen screen, CallbackInfo ci) {
		if (screen != null && screen.getClass() == TitleScreen.class) {
			if (AmazingTitleScreen.bypassOnce) {
				AmazingTitleScreen.bypassOnce = false;
				return;
			}
			ci.cancel();
			((MinecraftClient) (Object) this).setScreen(new AmazingTitleScreen());
		}
	}
}
