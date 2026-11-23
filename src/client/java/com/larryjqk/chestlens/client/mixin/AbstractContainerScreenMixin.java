package com.larryjqk.chestlens.client.mixin;

import com.larryjqk.chestlens.client.ChestLensClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

	@Inject(method = "extractTooltip", at = @At("HEAD"))
	private void chestLens$drawFilterBeforeTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
		ChestLensClient.drawFilter((AbstractContainerScreen<?>) (Object) this, graphics);
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void chestLens$handleKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (ChestLensClient.handleKey((AbstractContainerScreen<?>) (Object) this, event)) {
			cir.setReturnValue(true);
		}
	}
}