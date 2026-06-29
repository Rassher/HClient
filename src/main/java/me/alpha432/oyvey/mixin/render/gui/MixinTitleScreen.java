package me.alpha432.oyvey.mixin.render.gui;

import me.alpha432.oyvey.features.gui.title.HClientTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {

    /**
     * When vanilla TitleScreen is initializing and we are NOT already in our
     * custom screen, redirect to HClientTitleScreen.
     * The instanceof check prevents infinite recursion because
     * HClientTitleScreen.super.init() → TitleScreen.init() fires this again,
     * but mc.screen is already HClientTitleScreen at that point.
     */
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void hclient$redirect(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof HClientTitleScreen)) {
            mc.setScreen(new HClientTitleScreen());
            ci.cancel();
        }
    }
}
