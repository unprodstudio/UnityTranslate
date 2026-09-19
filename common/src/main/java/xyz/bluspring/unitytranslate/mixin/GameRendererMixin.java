package xyz.bluspring.unitytranslate.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.bluspring.unitytranslate.client.renderer.BatchedGuiRenderer;
import xyz.bluspring.unitytranslate.client.renderer.UnityTranslateGui;
import xyz.bluspring.unitytranslate.client.renderer.ui.BatchedUIGraphics;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "extract", at = @At("TAIL"))
    private void extractBatchedScreen(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        var graphics = new BatchedUIGraphics(BatchedGuiRenderer.DrawLayer.SCREEN);
        UnityTranslateGui.INSTANCE.submitLate(graphics, deltaTracker.getGameTimeDeltaPartialTick(true));
        graphics.flushLastLayer();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V"))
    private void renderBatchedGui(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        BatchedGuiRenderer.INSTANCE.render(BatchedGuiRenderer.DrawLayer.IN_GAME);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V", shift = At.Shift.AFTER))
    private void renderBatchedScreen(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        BatchedGuiRenderer.INSTANCE.render(BatchedGuiRenderer.DrawLayer.SCREEN);
    }
}
