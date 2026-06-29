package me.alpha432.oyvey.features.modules.client;

import com.google.gson.JsonElement;
import me.alpha432.oyvey.event.impl.input.KeyInputEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;

import java.util.ArrayList;
import java.util.List;

public class MacroModule extends Module {

    /** Serialized as "keyCodecommandkeyCode2command2..." */
    public final Setting<String> data = str("Data", "");

    public final List<MacroEntry> entries = new ArrayList<>();

    private static final String RS = ""; // record separator
    private static final String US = ""; // unit separator

    public MacroModule() {
        super("Macros", "Bind commands to hotkeys", Category.MISC);
    }

    @Override
    public void fromJson(JsonElement element) {
        super.fromJson(element); // loads all settings (including data) first
        deserialize();           // then rebuild entries from the loaded data
    }

    // ── Key execution ─────────────────────────────────────────────────────────

    @Subscribe
    public void onKey(KeyInputEvent e) {
        if (mc.screen != null) return;
        int key = e.getKey();
        for (MacroEntry entry : entries) {
            if (entry.key == key && !entry.command.isBlank()) {
                execute(entry.command);
                return;
            }
        }
    }

    private void execute(String command) {
        String cmd = command.trim();
        if (cmd.startsWith("/")) {
            mc.player.connection.sendCommand(cmd.substring(1));
        } else {
            mc.player.connection.sendChat(cmd);
        }
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public void serialize() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            MacroEntry e = entries.get(i);
            if (i > 0) sb.append(RS);
            sb.append(e.key).append(US)
              .append(e.command.replace(RS, "").replace(US, ""));
        }
        data.setValue(sb.toString());
    }

    private void deserialize() {
        entries.clear();
        String raw = data.getValue();
        if (raw == null || raw.isBlank()) return;
        for (String record : raw.split(RS, -1)) {
            String[] parts = record.split(US, 2);
            if (parts.length < 2) continue;
            try {
                int key = Integer.parseInt(parts[0]);
                entries.add(new MacroEntry(key, parts[1]));
            } catch (NumberFormatException ignored) {}
        }
    }

    // ── Data model ────────────────────────────────────────────────────────────

    public static class MacroEntry {
        public int    key;
        public String command;
        public MacroEntry(int key, String command) { this.key = key; this.command = command; }
        public MacroEntry() { this(-1, ""); }
    }

    public static String keyName(int key) {
        if (key < 0) return "NONE";
        String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(key, 0);
        if (name != null && !name.isBlank()) return name.toUpperCase();
        return switch (key) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE        -> "SPACE";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT   -> "LSHIFT";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT  -> "RSHIFT";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT     -> "LALT";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_TAB          -> "TAB";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_CAPS_LOCK    -> "CAPS";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE       -> "ESC";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER        -> "ENTER";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE    -> "BKSP";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE       -> "DEL";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_UP           -> "UP";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN         -> "DOWN";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT         -> "LEFT";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT        -> "RIGHT";
            default -> "KEY" + key;
        };
    }
}
