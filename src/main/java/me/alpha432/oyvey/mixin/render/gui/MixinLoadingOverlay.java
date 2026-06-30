package me.alpha432.oyvey.mixin.render.gui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Field;

@Mixin(LoadingOverlay.class)
public class MixinLoadingOverlay {

    private static Field FIELD_MC;
    private static Field FIELD_RELOAD;

    static {
        try {
            for (Field f : LoadingOverlay.class.getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getType() == Minecraft.class)      FIELD_MC    = f;
                if (f.getType() == ReloadInstance.class) FIELD_RELOAD = f;
            }
        } catch (Exception ignored) {}
    }

    // Loaded once from classpath — bypasses the not-yet-ready resource pack system
    private static ResourceLocation hclient$bgLoc = null;
    private static boolean          hclient$bgTried = false;

    private float hclient$progress = 0f;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hclient$render(GuiGraphics g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (FIELD_MC == null || FIELD_RELOAD == null) return;

        Minecraft mc;
        ReloadInstance reload;
        try {
            mc     = (Minecraft)      FIELD_MC.get(this);
            reload = (ReloadInstance) FIELD_RELOAD.get(this);
        } catch (Exception e) { return; }

        ci.cancel();

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        // Smooth progress
        float target = reload.getActualProgress();
        hclient$progress += (target - hclient$progress) * 0.1f;
        float p = Math.min(hclient$progress, 1.0f);

        // Load background texture from classpath on first render
        if (!hclient$bgTried) {
            hclient$bgTried = true;
            try (InputStream in = MixinLoadingOverlay.class
                    .getResourceAsStream("/assets/oyvey/textures/gui/loading_bg.png")) {
                if (in != null) {
                    NativeImage img = NativeImage.read(in);
                    DynamicTexture tex = new DynamicTexture(img);
                    hclient$bgLoc = mc.getTextureManager().register("hclient_loading_bg", tex);
                }
            } catch (Exception e) {
                LoggerFactory.getLogger("HClient").warn("Could not load loading background: {}", e.getMessage());
            }
        }

        // Draw background
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        if (hclient$bgLoc != null) {
            g.blit(hclient$bgLoc, 0, 0, sw, sh, 0f, 0f, 2752, 1536, 2752, 1536);
        } else {
            g.fill(0, 0, sw, sh, 0xFF0D0D0D); // fallback dark background
        }

        // Progress bar
        int barW = (int)(sw * 0.45f);
        int barH = 8;
        int barX = sw / 2 - barW / 2;
        int barY = sh - 38;
        int fill = (int)(barW * p);

        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF3A5A2A);
        g.fill(barX,     barY,     barX + barW,      barY + barH,     0xFF1A1A1A);
        if (fill > 0) {
            g.fill(barX, barY, barX + fill, barY + barH, 0xFF4A8A3A);
            g.fill(barX, barY, barX + fill, barY + 2,    0xFF6ABF55);
        }

        g.drawCenteredString(mc.font, (int)(p * 100) + "%", sw / 2, barY - 12, 0xFFAADD88);
    }
}
