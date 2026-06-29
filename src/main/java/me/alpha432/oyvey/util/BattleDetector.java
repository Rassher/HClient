package me.alpha432.oyvey.util;

import net.minecraft.client.Minecraft;

public final class BattleDetector {

    private static final Class<?> BATTLE_GUI;

    static {
        Class<?> c = null;
        for (String name : new String[]{
                "com.cobblemon.mod.common.client.gui.battle.BattleGUI",
                "com.cobblemon.mod.common.client.gui.battle.BattleScreen"}) {
            try { c = Class.forName(name); break; }
            catch (ClassNotFoundException ignored) {}
        }
        BATTLE_GUI = c;
    }

    private BattleDetector() {}

    public static boolean isInBattle() {
        var screen = Minecraft.getInstance().screen;
        if (screen == null) return false;
        if (BATTLE_GUI != null) return BATTLE_GUI.isInstance(screen);
        // fallback: name-based check
        return screen.getClass().getName().toLowerCase().contains("battle");
    }
}
