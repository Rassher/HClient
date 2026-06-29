package me.alpha432.oyvey.features.modules.hud;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.client.CobblemonSpawnModule;
import me.alpha432.oyvey.features.modules.client.HudModule;
import me.alpha432.oyvey.util.PokemonSpriteCache;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class CobblemonActiveHudModule extends HudModule {

    private static final List<String> RARITY_ORDER = List.of("ultra-rare", "rare", "uncommon", "common");
    private static final int ICON   = 32;
    private static final int VGAP   = 2;  // gap between icon and text
    private static final int HGAP   = 8;  // gap between entries

    private static final class Entry {
        final String name;
        final int dexNumber;
        int count;
        boolean shiny;
        String rarity;
        Entry(String name, int dexNumber, int count, boolean shiny, String rarity) {
            this.name = name; this.dexNumber = dexNumber;
            this.count = count; this.shiny = shiny; this.rarity = rarity;
        }
    }

    public CobblemonActiveHudModule() {
        super("CobblemonActive", "Active Cobblemon spawns overlay", 80, 42);
    }

    @Override
    protected void render(Render2DEvent e) {
        super.render(e);

        CobblemonSpawnModule spawn = OyVey.moduleManager.getModuleByClass(CobblemonSpawnModule.class);
        if (spawn == null || !spawn.isEnabled() || spawn.tracked.isEmpty()) {
            setWidth(80); setHeight(42); return;
        }

        // Group by species
        LinkedHashMap<String, Entry> grouped = new LinkedHashMap<>();
        for (CobblemonSpawnModule.TrackedPokemon tp : spawn.tracked.values()) {
            Entry ex = grouped.get(tp.name());
            if (ex == null) {
                grouped.put(tp.name(), new Entry(tp.name(), tp.dexNumber(), 1, tp.shiny(), tp.rarity()));
            } else {
                ex.count++;
                if (tp.shiny()) ex.shiny = true;
                if (RARITY_ORDER.indexOf(tp.rarity()) < RARITY_ORDER.indexOf(ex.rarity)) ex.rarity = tp.rarity();
            }
        }

        List<Entry> sorted = new ArrayList<>(grouped.values());
        sorted.sort(Comparator
                .<Entry, Integer>comparing(en -> en.shiny ? -1 : RARITY_ORDER.indexOf(en.rarity))
                .thenComparing(en -> en.name));

        float startX = getX();
        float startY = getY();
        int entryH = ICON + VGAP + mc.font.lineHeight;

        // Total widget width
        float totalW = 0;
        for (int i = 0; i < sorted.size(); i++) {
            float entW = Math.max(ICON, mc.font.width(toLabel(sorted.get(i))));
            totalW += entW;
            if (i < sorted.size() - 1) totalW += HGAP;
        }
        setWidth(totalW);
        setHeight(entryH);

        float x = startX;
        for (Entry en : sorted) {
            String lbl   = toLabel(en);
            float textW  = mc.font.width(lbl);
            float entW   = Math.max(ICON, textW);
            float iconX  = x + (entW - ICON) / 2f;
            float textX  = x + (entW - textW) / 2f;

            // Icon (32×32, scaled from 96×96 via pose)
            ResourceLocation sprite = PokemonSpriteCache.get(en.dexNumber, en.shiny);
            if (sprite != null) {
                var pose = e.getContext().pose();
                pose.pushPose();
                pose.translate(iconX, startY, 0f);
                pose.scale(ICON / 96f, ICON / 96f, 1f);
                e.getContext().blit(sprite, 0, 0, 0, 0, 96, 96, 96, 96);
                pose.popPose();
            }

            // Text below icon
            int color = rarityColor(en.rarity, en.shiny);
            e.getContext().drawString(mc.font, lbl, (int) textX, (int)(startY + ICON + VGAP), color);

            x += entW + HGAP;
        }
    }

    private static String toLabel(Entry en) {
        String s = (en.shiny ? "§e★ §r" : "") + capitalize(en.name);
        if (en.count > 1) s += " §7x" + en.count;
        return s;
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static int rarityColor(String rarity, boolean shiny) {
        if (shiny) return 0xFFDD44;
        return switch (rarity) {
            case "ultra-rare" -> 0xFF55FF;
            case "rare"       -> 0x55FFFF;
            case "uncommon"   -> 0x55FF55;
            default           -> 0xFFFFFF;
        };
    }
}
