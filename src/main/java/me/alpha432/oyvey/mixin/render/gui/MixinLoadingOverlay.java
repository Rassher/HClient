package me.alpha432.oyvey.mixin.render.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public class MixinLoadingOverlay {

    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath("oyvey", "textures/gui/loading_bg.png");

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private ReloadInstance reload;
    @Shadow private float currentProgress;
    @Shadow private long fadeOutStart;   // -1 = not fading out
    @Shadow @Final private boolean fadeIn;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hclient$render(GuiGraphics g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ci.cancel();

        int sw = this.minecraft.getWindow().getGuiScaledWidth();
        int sh = this.minecraft.getWindow().getGuiScaledHeight();

        // Smooth progress
        float target = this.reload.getActualProgress();
        this.currentProgress = this.currentProgress + (target - this.currentProgress) * 0.1f;

        // Alpha: fade out when fadeOutStart is set (>= 0)
        float alpha = 1.0f;
        if (this.fadeOutStart >= 0) {
            long elapsed = System.currentTimeMillis() - this.fadeOutStart;
            alpha = 1.0f - Math.min(1.0f, elapsed / 1000.0f);
        }

        int a = (int)(alpha * 255) << 24;

        // Background image stretched to fill screen
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        g.blit(BG, 0, 0, sw, sh, 0f, 0f, 2752, 1536, 2752, 1536);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        // --- Progress bar ---
        int barW  = (int)(sw * 0.45f);
        int barH  = 8;
        int barX  = sw / 2 - barW / 2;
        int barY  = sh - 38;
        int fill  = (int)(barW * Math.min(this.currentProgress, 1.0f));

        int trackColor  = applyAlpha(0x1A1A1A, a);
        int borderColor = applyAlpha(0x3A5A2A, a);
        int fillColor   = applyAlpha(0x4A8A3A, a);
        int glowColor   = applyAlpha(0x6ABF55, a);
        int textColor   = applyAlpha(0xAADD88, a);

        // Border
        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, borderColor);
        // Track
        g.fill(barX, barY, barX + barW, barY + barH, trackColor);
        // Fill
        if (fill > 0) {
            g.fill(barX, barY, barX + fill, barY + barH, fillColor);
            g.fill(barX, barY, barX + fill, barY + 2, glowColor); // highlight
        }

        // Percentage
        int pct = (int)(Math.min(this.currentProgress, 1.0f) * 100);
        g.drawCenteredString(this.minecraft.font, pct + "%", sw / 2, barY - 12, textColor);
    }

    private static int applyAlpha(int rgb, int alphaBits) {
        return (alphaBits & 0xFF000000) | (rgb & 0x00FFFFFF);
    }
}
