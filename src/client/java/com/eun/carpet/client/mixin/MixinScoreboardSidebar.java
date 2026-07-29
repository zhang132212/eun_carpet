package com.eun.carpet.client.mixin;

import com.eun.carpet.client.feature.ScoreboardHider;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class MixinScoreboardSidebar {

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void onExtractScoreboardSidebar(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ScoreboardHider.isHidden()) {
            ci.cancel();
        }
    }
}
