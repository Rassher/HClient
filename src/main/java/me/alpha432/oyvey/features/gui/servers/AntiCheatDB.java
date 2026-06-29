package me.alpha432.oyvey.features.gui.servers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AntiCheatDB {

    private static final Map<String, Integer> LEVELS = new HashMap<>();
    private static boolean loaded = false;

    public static void load() {
        if (loaded) return;
        loaded = true;
        try (InputStream is = AntiCheatDB.class.getResourceAsStream("/anticheatlvl.txt")) {
            if (is == null) return;
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.contains("|")) continue;
                String[] parts = line.split("\\|");
                if (parts.length < 4) continue;
                String ip = parts[2].trim().toLowerCase();
                String lvlStr = parts[3].trim();
                if (lvlStr.isEmpty() || !Character.isDigit(lvlStr.charAt(0))) continue;
                int lvl = Character.getNumericValue(lvlStr.charAt(0));
                if (!ip.isEmpty()) LEVELS.put(ip, lvl);
            }
        } catch (Exception ignored) {}
    }

    /** Returns 1-3 anticheat level, or -1 if the server is unknown. */
    public static int getLevel(String ip) {
        if (!loaded) load();
        if (ip == null || ip.isEmpty()) return -1;
        String key = ip.toLowerCase().trim();
        String domain = key.contains(":") ? key.substring(0, key.indexOf(':')) : key;

        Integer exact = LEVELS.get(domain);
        if (exact != null) return exact;

        for (Map.Entry<String, Integer> e : LEVELS.entrySet()) {
            String dbDomain = e.getKey();
            if (domain.endsWith(dbDomain) || dbDomain.endsWith(domain)) return e.getValue();
        }
        return -1;
    }

    public static String getLevelLabel(int lvl) {
        return switch (lvl) {
            case 1 -> "Seguro";
            case 2 -> "Cuidado";
            case 3 -> "Peligro";
            default -> "Sin Analizar";
        };
    }

    public static int getLevelColor(int lvl) {
        return switch (lvl) {
            case 1 -> 0xFF55FF55;
            case 2 -> 0xFFFFAA00;
            case 3 -> 0xFFFF5555;
            default -> 0xFF8888FF;
        };
    }

    public static boolean isRisky(int lvl) {
        return lvl == 3 || lvl == -1;
    }
}
