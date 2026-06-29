package me.alpha432.oyvey.mixin.network;

import me.alpha432.oyvey.event.impl.network.ChatEvent;
import me.alpha432.oyvey.event.impl.network.ChatReceiveEvent;
import me.alpha432.oyvey.event.impl.network.ContainerOpenEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static me.alpha432.oyvey.util.traits.Util.EVENT_BUS;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {

    private Component hclient$lastScreenTitle = null;

    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void sendChat(String content, CallbackInfo ci) {
        if (EVENT_BUS.post(new ChatEvent(content))) {
            ci.cancel();
        }
    }

    @Inject(method = "handleSystemChat", at = @At("HEAD"))
    private void handleSystemChat(net.minecraft.network.protocol.game.ClientboundSystemChatPacket packet, CallbackInfo ci) {
        EVENT_BUS.post(new ChatReceiveEvent(packet.content(), packet.overlay()));
    }

    /** Capture the screen title when a container is opened. */
    @Inject(method = "handleOpenScreen", at = @At("HEAD"))
    private void handleOpenScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        hclient$lastScreenTitle = packet.getTitle();
    }

    /** When the server fills a container with items, fire ContainerOpenEvent with title + items. */
    @Inject(method = "handleContainerContent", at = @At("HEAD"))
    private void handleContainerContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        // Container ID 0 = player inventory — not a GUI we care about
        if (packet.getContainerId() == 0) return;

        Component title = hclient$lastScreenTitle;
        if (title == null) {
            var screen = Minecraft.getInstance().screen;
            if (screen != null) {
                try { title = (Component) screen.getClass().getMethod("getTitle").invoke(screen); }
                catch (Exception ignored) {}
            }
        }
        if (title == null) title = Component.literal("Unknown");

        EVENT_BUS.post(new ContainerOpenEvent(title, packet.getItems()));
    }
}
