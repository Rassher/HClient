package me.alpha432.oyvey.mixin.render.gui;

import me.alpha432.oyvey.features.gui.servers.HClientServerListScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class MixinJoinMultiplayerScreen {

    @Shadow private Screen lastScreen;

    @Inject(method = "init", at = @At("RETURN"))
    private void hclient$redirect(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof HClientServerListScreen)) {
            mc.setScreen(new HClientServerListScreen(lastScreen));
        }
    }
}
