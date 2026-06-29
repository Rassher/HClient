package me.alpha432.oyvey.util;

import me.alpha432.oyvey.event.impl.network.ChatReceiveEvent;
import me.alpha432.oyvey.event.impl.network.ContainerOpenEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Logs incoming chat AND container GUIs to pokehunt_debug.txt.
 * Used to reverse-engineer the /pokehunt server format.
 */
public class PokeHuntDebugLogger implements Util {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Path LOG_FILE = Path.of("pokehunt_debug.txt");
    private boolean active = false;

    public void start() {
        if (active) return;
        active = true;
        EVENT_BUS.register(this);
        log("=== PokeHunt debug session started " + LocalDateTime.now() + " ===");
    }

    public void stop() {
        if (!active) return;
        active = false;
        EVENT_BUS.unregister(this);
        log("=== Session ended " + LocalDateTime.now() + " ===\n");
    }

    public boolean isActive() { return active; }

    // ── Chat messages ─────────────────────────────────────────────────────────

    @Subscribe
    public void onChat(ChatReceiveEvent e) {
        String plain = e.getMessage();
        // Only log hunt-related chat to keep file clean
        String lower = plain.toLowerCase();
        if (lower.contains("hunt") || lower.contains("pokehunt")) {
            log("[CHAT] [overlay=" + e.isOverlay() + "] " + plain);
            log("  JSON: " + Component.Serializer.toJson(e.getComponent(), mc.level.registryAccess()));
        }
    }

    // ── Container GUIs ────────────────────────────────────────────────────────

    @Subscribe
    public void onContainer(ContainerOpenEvent e) {
        String title = e.getTitleText();
        List<ItemStack> items = e.getItems();

        log("\n[GUI] title=|" + title + "|  slots=" + items.size());

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;

            String name = stack.getHoverName().getString();
            log("  slot[" + i + "] item=" + stack.getItem() + "  name=|" + name + "|  count=" + stack.getCount());

            // Lore lines
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore != null && !lore.lines().isEmpty()) {
                for (int l = 0; l < lore.lines().size(); l++) {
                    log("    lore[" + l + "]: |" + lore.lines().get(l).getString() + "|");
                }
            }

            // Custom name raw JSON (contains color/format info)
            var customName = stack.get(DataComponents.CUSTOM_NAME);
            if (customName != null) {
                log("    custom_name_json: " + Component.Serializer.toJson(customName, mc.level.registryAccess()));
            }
        }
    }

    private void log(String line) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE.toFile(), true))) {
            pw.println(line);
        } catch (IOException ignored) {}
    }
}
