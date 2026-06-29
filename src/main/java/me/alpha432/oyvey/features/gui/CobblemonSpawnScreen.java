package me.alpha432.oyvey.features.gui;

import me.alpha432.oyvey.features.modules.client.CobblemonSpawnModule;
import me.alpha432.oyvey.util.PokemonSpriteCache;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.awt.Color;
import java.util.*;
import java.util.stream.Collectors;

public class CobblemonSpawnScreen extends Screen {

    private final CobblemonSpawnModule module;

    // Panel geometry
    private int px, py, pw, ph;
    private int settingsX, settingsW;
    private int filterX, filterW;

    // Pokemon list
    private record PokemonEntry(String name, String rarity, int dexNumber) {}
    private final List<PokemonEntry> allEntries = new ArrayList<>();
    private List<PokemonEntry> visible = new ArrayList<>();
    private int listScrollOffset = 0;
    private static final int ROW_H = 11;
    private static final int LIST_ROWS = 14;

    // Preview
    private String previewName = null;
    private final Map<String, LivingEntity> entityCache = new HashMap<>();

    // Widgets
    private EditBox searchBox;
    private boolean draggingRange = false;

    // ── Particle system ───────────────────────────────────────────────────────

    private static final int   PARTICLE_COUNT = 50;
    private static final float CONNECT_DIST   = 90f;
    private static final float BASE_SPEED     = 0.022f;

    private static class Particle {
        float x, y, vx, vy;
        Particle(float x, float y, float vx, float vy) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        }
    }

    private final List<Particle> particles = new ArrayList<>();
    private float rainbowHue = 0f;
    private long lastFrameNs = System.nanoTime();

    // ── Constructor ───────────────────────────────────────────────────────────

    public CobblemonSpawnScreen(CobblemonSpawnModule module) {
        super(Component.literal("CobblemonSpawn Settings"));
        this.module = module;
    }

    @Override
    protected void init() {
        pw = Math.min(width - 40, 680);
        ph = Math.min(height - 40, 360);
        px = (width - pw) / 2;
        py = (height - ph) / 2;

        settingsX = px + 8;
        settingsW = pw / 2 - 16;
        filterX   = px + pw / 2 + 8;
        filterW   = pw / 2 - 16;

        // Spawn particles randomly inside the panel body
        particles.clear();
        Random rnd = new Random();
        int innerW = pw - 4, innerH = ph - 22;
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float x  = px + 2 + rnd.nextFloat() * innerW;
            float y  = py + 21 + rnd.nextFloat() * innerH;
            double ang = rnd.nextDouble() * Math.PI * 2;
            float spd = BASE_SPEED * (0.5f + rnd.nextFloat());
            particles.add(new Particle(x, y, (float)Math.cos(ang)*spd, (float)Math.sin(ang)*spd));
        }

        allEntries.clear();
        if (!tryLoadFromCobblemon()) loadFromHardcodedSets();
        // Ensure every pokémon already in the filter appears in the list,
        // even if it's "common" and wasn't included in the hardcoded sets.
        String existing = module.filter.getValue();
        if (!existing.isBlank()) {
            Set<String> known = new java.util.HashSet<>();
            allEntries.forEach(e -> known.add(e.name().toLowerCase()));
            for (String p : existing.split(",")) {
                String t = p.trim().toLowerCase();
                if (!t.isEmpty() && !known.contains(t)) {
                    allEntries.add(new PokemonEntry(t, getRarityLabel(t), 0));
                    known.add(t);
                }
            }
        }
        allEntries.sort(Comparator.comparing(PokemonEntry::name));
        visible = new ArrayList<>(allEntries);

        searchBox = new EditBox(minecraft.font,
                filterX, py + 39, filterW, 11,
                Component.literal("Search Pokémon..."));
        searchBox.setMaxLength(64);
        searchBox.setResponder(this::onSearch);
        searchBox.setBordered(false);
        addWidget(searchBox);

        lastFrameNs = System.nanoTime();
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private boolean tryLoadFromCobblemon() {
        try {
            Object reg = Class.forName("com.cobblemon.mod.common.api.pokemon.PokemonSpecies")
                    .getField("INSTANCE").get(null);
            for (java.lang.reflect.Method m : reg.getClass().getMethods()) {
                if (m.getName().equals("implemented") && m.getParameterCount() == 0) {
                    Iterable<?> species = (Iterable<?>) m.invoke(reg);
                    for (Object s : species) {
                        String name = (String) s.getClass().getMethod("getName").invoke(s);
                        int dex = 0;
                        for (String m2 : new String[]{"getNationalPokedexNumber","nationalPokedexNumber"}) {
                            try { dex = (int) s.getClass().getMethod(m2).invoke(s); break; }
                            catch (Exception ignored2) {}
                        }
                        allEntries.add(new PokemonEntry(name, getRarityLabel(name), dex));
                    }
                    return !allEntries.isEmpty();
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void loadFromHardcodedSets() {
        Set<String> all = new LinkedHashSet<>();
        all.addAll(CobblemonSpawnModule.LEGENDARIES);
        all.addAll(CobblemonSpawnModule.PSEUDO_LEGENDARIES);
        all.addAll(CobblemonSpawnModule.UNCOMMONS);
        for (String n : all) allEntries.add(new PokemonEntry(n, getRarityLabel(n), 0));
    }

    private void onSearch(String query) {
        if (query.isBlank()) {
            visible = new ArrayList<>(allEntries);
        } else {
            String q = query.toLowerCase().trim();
            visible = allEntries.stream()
                    .filter(e -> e.name().toLowerCase().contains(q))
                    .collect(Collectors.toCollection(ArrayList::new));
            // If the exact typed name isn't in the list yet, add a virtual entry
            // so the user can click it to add it to the filter (e.g. a common pokémon)
            boolean exactMatch = visible.stream()
                    .anyMatch(e -> e.name().equalsIgnoreCase(q));
            if (!exactMatch && !q.isEmpty()) {
                visible.add(0, new PokemonEntry(q, getRarityLabel(q), 0));
            }
        }
        listScrollOffset = 0;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // Outer dim
        g.fill(0, 0, width, height, 0x88000000);

        // Panel shadow
        g.fill(px + 4, py + 4, px + pw + 4, py + ph + 4, 0x55000000);

        // Solid base
        g.fill(px, py, px + pw, py + ph, new Color(10, 8, 16, 255).getRGB());

        // ── Semi-transparent overlay (dims base, particles go ON TOP of this) ──
        // 90% overlay to dim the base behind the UI
        g.fill(px, py, px + pw, py + ph, 0xE6080610);

        // Particles rendered after overlay — scissor flushes above fills first
        updateAndDrawParticles(g);

        // ── Title bar (fully opaque on top of overlay) ──
        g.fill(px, py, px + pw, py + 20, new Color(18, 14, 30, 245).getRGB());
        g.fill(px, py + 20, px + pw, py + 21, new Color(70, 50, 110).getRGB());
        g.drawString(minecraft.font, "✦ CobblemonSpawn", px + 8, py + 6, 0xCCAAFF);

        // ── Close button ──
        int closeSize = 12;
        int closeX = px + pw - closeSize - 4;
        int closeY = py + (20 - closeSize) / 2;
        boolean hoverClose = mouseX >= closeX && mouseX < closeX + closeSize
                          && mouseY >= closeY && mouseY < closeY + closeSize;
        int closeBg = hoverClose ? new Color(180, 50, 50, 220).getRGB() : new Color(55, 45, 75).getRGB();
        g.fill(closeX, closeY, closeX + closeSize, closeY + closeSize, closeBg);
        drawBorder(g, closeX, closeY, closeSize, closeSize,
                hoverClose ? 0xFFCC4444 : new Color(80, 65, 105).getRGB());
        int xGlyphX = closeX + (closeSize - minecraft.font.width("✕")) / 2;
        int xGlyphY = closeY + (closeSize - minecraft.font.lineHeight) / 2 + 1;
        g.drawString(minecraft.font, "✕", xGlyphX, xGlyphY, hoverClose ? 0xFFFFFF : 0xCCCCCC, false);

        // Divider
        g.fill(px + pw / 2, py + 21, px + pw / 2 + 1, py + ph - 1, new Color(45, 40, 65).getRGB());

        renderSettings(g, mouseX, mouseY);
        renderFilterPanel(g, mouseX, mouseY);

        // Outer border
        drawBorder(g, px, py, pw, ph, new Color(55, 45, 80).getRGB());

        super.render(g, mouseX, mouseY, delta);
    }

    // ── Particle animation ────────────────────────────────────────────────────

    private void updateAndDrawParticles(GuiGraphics g) {
        long now = System.nanoTime();
        float dt = Math.min((now - lastFrameNs) / 1_000_000f, 50f);
        lastFrameNs = now;

        rainbowHue = (rainbowHue + 0.35f * dt / 16.67f) % 360f;

        float speedMult = 5f;
        boolean rgb     = true;

        float minX = px + 2, maxX = px + pw - 2;
        float minY = py + 22, maxY = py + ph - 2;

        for (Particle p : particles) {
            p.x += p.vx * dt * speedMult;
            p.y += p.vy * dt * speedMult;
            if (p.x < minX) { p.x = minX; p.vx =  Math.abs(p.vx); }
            if (p.x > maxX) { p.x = maxX; p.vx = -Math.abs(p.vx); }
            if (p.y < minY) { p.y = minY; p.vy =  Math.abs(p.vy); }
            if (p.y > maxY) { p.y = maxY; p.vy = -Math.abs(p.vy); }
        }

        // scissor flush queued fills first, then clips particles to panel bounds
        g.enableScissor((int)minX, (int)minY, (int)maxX, (int)maxY);

        // ── Lines first (behind dots) ──
        int n = particles.size(), drawn = 0;
        for (int i = 0; i < n && drawn < 60; i++) {
            Particle a = particles.get(i);
            for (int j = i + 1; j < n && drawn < 60; j++) {
                Particle b = particles.get(j);
                float ddx = b.x - a.x, ddy = b.y - a.y;
                float dist = (float) Math.sqrt(ddx*ddx + ddy*ddy);
                if (dist < CONNECT_DIST) {
                    float alpha = (1f - dist / CONNECT_DIST) * 0.75f;
                    float h = (rainbowHue + ((a.x + b.x) * 0.5f - px) / pw * 80f) % 360f;
                    int col = hslToArgb(h, 1f, 0.65f, alpha);
                    drawLineWithFill(g, a.x, a.y, b.x, b.y, col);
                    drawn++;
                }
            }
        }

        // ── Dots (on top of lines) ──
        for (Particle p : particles) {
            float h = (rainbowHue + ((p.x - px) / pw) * 80f) % 360f;
            int dot = hslToArgb(h, 1f, 0.75f, 0.90f);
            g.fill((int)p.x - 1, (int)p.y - 1, (int)p.x + 2, (int)p.y + 2, dot);
        }

        g.disableScissor();
    }

    private static int hslToArgb(float h, float s, float l, float alpha) {
        float c = (1f - Math.abs(2f*l - 1f)) * s;
        float x = c * (1f - Math.abs((h/60f) % 2f - 1f));
        float m = l - c/2f;
        float r, gr, b;
        if      (h < 60)  { r=c; gr=x; b=0; }
        else if (h < 120) { r=x; gr=c; b=0; }
        else if (h < 180) { r=0; gr=c; b=x; }
        else if (h < 240) { r=0; gr=x; b=c; }
        else if (h < 300) { r=x; gr=0; b=c; }
        else              { r=c; gr=0; b=x; }
        int ri = (int)((r+m)*255), gi = (int)((gr+m)*255), bi = (int)((b+m)*255), ai = (int)(alpha*255);
        return (ai<<24)|(ri<<16)|(gi<<8)|bi;
    }

    // ── Settings panel ────────────────────────────────────────────────────────

    private void renderSettings(GuiGraphics g, int mouseX, int mouseY) {
        int x = settingsX;
        int y = py + 26;

        g.drawString(minecraft.font, "Settings", x, y, 0x9988BB);
        y += 13;

        // Range — bar + inline number, both stay within settingsW
        int range    = module.range.getValue();
        int numW     = minecraft.font.width("256m") + 2; // fixed max width
        int labelW   = 38;                               // "Range " label width + gap
        int barW     = settingsW - labelW - numW - 6;    // remaining space
        int barX     = x + labelW;
        int barFill  = (int)(barW * range / 256f);

        g.drawString(minecraft.font, "Range", x, y + 1, 0xAAAAAA);
        g.fill(barX, y, barX + barW, y + 9, new Color(32, 30, 48).getRGB());
        g.fill(barX, y, barX + barFill, y + 9, new Color(75, 55, 135).getRGB());
        drawBorder(g, barX, y, barW, 9, new Color(58, 52, 78).getRGB());
        g.drawString(minecraft.font, range + "m", barX + barW + 3, y + 1, 0xCCCCCC);
        y += 14;

        // Min Rarity
        CobblemonSpawnModule.Rarity rarity = module.minRarity.getValue();
        g.drawString(minecraft.font, "Min Rarity", x, y + 1, 0xAAAAAA);
        int btnX = x + 60;
        int btnW = minecraft.font.width(rarity.name()) + 10;
        boolean hRar = mouseX >= btnX && mouseX <= btnX+btnW && mouseY >= y-1 && mouseY <= y+10;
        g.fill(btnX, y-1, btnX+btnW, y+10, hRar ? new Color(58,52,78).getRGB() : new Color(35,32,52).getRGB());
        drawBorder(g, btnX, y-1, btnW, 11, new Color(68, 58, 98).getRGB());
        g.drawString(minecraft.font, rarity.name(), btnX + 5, y + 1, rarityEnumColor(rarity));
        y += 14;

        // Shinies / ESP / HuntDebug
        y = drawCheckbox(g, x, y, "Shinies",    module.shinies.getValue(),    mouseX, mouseY);
        y = drawCheckbox(g, x, y, "ESP",        module.esp.getValue(),        mouseX, mouseY);
        y = drawCheckbox(g, x, y, "Sound",      module.sound.getValue(),      mouseX, mouseY);
        y = drawCheckbox(g, x, y, "Hunt Debug", module.huntDebug.getValue(),  mouseX, mouseY);

        y += 8;

        // Active filter chips
        g.drawString(minecraft.font, "Active filter:", x, y, 0x9988BB);
        y += 11;
        String filterStr = module.filter.getValue();
        if (filterStr.isBlank()) {
            g.drawString(minecraft.font, "All Pokémon", x, y, 0x555566);
        } else {
            int cx2 = x;
            for (String p : filterStr.split(",")) {
                p = p.trim();
                if (p.isEmpty()) continue;
                int cw = minecraft.font.width(p) + 8;
                if (cx2 + cw > settingsX + settingsW + 2) { cx2 = x; y += 12; }
                if (y > py + ph - 100) break;
                g.fill(cx2, y-1, cx2+cw, y+10, new Color(42, 36, 66).getRGB());
                drawBorder(g, cx2, y-1, cw, 11, new Color(78, 58, 135).getRGB());
                g.drawString(minecraft.font, p, cx2+4, y+1, 0xBBAAFF);
                cx2 += cw + 3;
            }
        }

    }

    /** Draws a line using g.fill() — no endBatch, stays in the deferred buffer. */
    private static void drawLineWithFill(GuiGraphics g, float x1, float y1, float x2, float y2, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx*dx + dy*dy);
        int steps = Math.max(1, (int)(len / 2f)); // one dot every 2 pixels
        for (int s = 0; s <= steps; s++) {
            float t = s / (float) steps;
            int lx = (int)(x1 + dx * t);
            int ly = (int)(y1 + dy * t);
            g.fill(lx, ly, lx + 1, ly + 1, color);
        }
    }

    // ── Filter panel ──────────────────────────────────────────────────────────

    private void renderFilterPanel(GuiGraphics g, int mouseX, int mouseY) {
        int x = filterX;
        int y = py + 26;

        g.drawString(minecraft.font, "Pokémon Filter", x, y, 0x9988BB);
        y += 13; // search box at py+39

        // Search box
        searchBox.setX(x);
        searchBox.setY(y);
        searchBox.setWidth(filterW);
        g.fill(x-1, y-1, x+filterW+1, y+12, new Color(28, 25, 42).getRGB());
        drawBorder(g, x-1, y-1, filterW+2, 13, new Color(65, 55, 95).getRGB());
        if (searchBox.getValue().isEmpty())
            g.drawString(minecraft.font, "Search...", x+3, y+1, 0x444455);

        y += 16; // list at py+55

        int listX = x-1, listY = y;
        int listW = filterW+2, listH = LIST_ROWS * ROW_H;

        g.fill(listX, listY, listX+listW, listY+listH, new Color(20, 18, 30).getRGB());
        drawBorder(g, listX, listY, listW, listH, new Color(42, 38, 60).getRGB());

        g.enableScissor(listX+1, listY+1, listX+listW-1, listY+listH-1);

        String hovered = null;
        for (int i = listScrollOffset; i < Math.min(visible.size(), listScrollOffset + LIST_ROWS); i++) {
            PokemonEntry entry = visible.get(i);
            int row = i - listScrollOffset;
            int ry  = listY + row * ROW_H;
            boolean isHov = mouseX >= listX+1 && mouseX <= listX+listW-1 && mouseY >= ry && mouseY < ry+ROW_H;
            boolean inFilter = isInFilter(entry.name());

            if (isHov) {
                hovered = entry.name();
                g.fill(listX+1, ry, listX+listW-1, ry+ROW_H, new Color(52, 44, 80).getRGB());
            } else if (inFilter) {
                g.fill(listX+1, ry, listX+listW-1, ry+ROW_H, new Color(32, 26, 56).getRGB());
            }

            // Pinned = common pokémon explicitly in the filter while minRarity > ALL
            boolean pinned = inFilter && entry.rarity().equals("common")
                    && module.minRarity.getValue() != CobblemonSpawnModule.Rarity.ALL;
            int nameColor = inFilter ? (pinned ? 0xFFAA00 : 0xCCAAFF) : (isHov ? 0xFFFFFF : 0xBBBBBB);

            // 2D sprite icon (9×9, scaled from 96×96)
            ResourceLocation sprite = PokemonSpriteCache.get(entry.dexNumber(), false);
            int nameOffsetX = 4;
            if (sprite != null) {
                var pose = g.pose();
                pose.pushPose();
                pose.translate(listX + 2, ry + 1, 0f);
                pose.scale(9f / 96f, 9f / 96f, 1f);
                g.blit(sprite, 0, 0, 0, 0, 96, 96, 96, 96);
                pose.popPose();
                nameOffsetX = 14;
            }
            g.drawString(minecraft.font, entry.name(), listX + nameOffsetX, ry + 2, nameColor);

            // Badge: pinned overrides rarity badge with a ★ pin icon
            if (pinned) {
                String pin = "★";
                g.drawString(minecraft.font, pin,
                        listX+listW - minecraft.font.width(pin) - 4, ry+2, 0xFFAA00);
            } else {
                String badge = rarityBadge(entry.rarity());
                if (!badge.isEmpty()) {
                    g.drawString(minecraft.font, badge,
                            listX+listW - minecraft.font.width(badge) - 4, ry+2,
                            rarityStrColor(entry.rarity()));
                }
            }
        }

        g.disableScissor();

        // Scroll bar
        if (visible.size() > LIST_ROWS) {
            int sbH = Math.max(listH * LIST_ROWS / visible.size(), 12);
            int sbY = listY + (listH - sbH) * listScrollOffset / Math.max(1, visible.size() - LIST_ROWS);
            g.fill(listX+listW-3, sbY, listX+listW-1, sbY+sbH, new Color(75, 60, 115).getRGB());
        }

        // Preview
        int previewY = listY + listH + 6;
        String toPreview = hovered != null ? hovered : previewName;
        if (toPreview != null) {
            int dex = allEntries.stream()
                    .filter(e -> e.name().equals(toPreview))
                    .mapToInt(PokemonEntry::dexNumber).findFirst().orElse(0);
            renderPreview(g, x, previewY, filterW, toPreview, dex, mouseX, mouseY);
        }
    }

    private void renderPreview(GuiGraphics g, int x, int y, int w, String name, int dexNumber, int mx, int my) {
        int remaining = py + ph - y - 4;
        if (remaining < 28) return;

        int boxSize = Math.min(remaining - 16, 68);
        int cx = x + w / 2;
        int bx1 = cx - boxSize/2, by1 = y, bx2 = cx + boxSize/2, by2 = y + boxSize;

        g.fill(bx1-2, by1-2, bx2+2, by2+2, new Color(22, 18, 35).getRGB());
        drawBorder(g, bx1-2, by1-2, boxSize+4, boxSize+4, new Color(58, 48, 88).getRGB());

        // 3D entity render
        LivingEntity entity = getOrBuildEntity(name);
        if (entity != null) {
            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        g, bx1, by1, bx2, by2, boxSize / 2, 0f,
                        (float) mx, (float)(by1 + boxSize * 0.2f), entity);
            } catch (Exception ex) {
                renderSpriteFallback(g, bx1, by1, boxSize, cx, name, dexNumber);
            }
        } else {
            renderSpriteFallback(g, bx1, by1, boxSize, cx, name, dexNumber);
        }

        int textY = by2 + 4;
        String dispName = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        String rarity   = getRarityLabel(name);
        g.drawCenteredString(minecraft.font, dispName, cx, textY, 0xFFFFFF);
        if (textY + 10 < py + ph - 4)
            g.drawCenteredString(minecraft.font, rarity, cx, textY + 10, rarityStrColor(rarity));
    }

    private void renderSpriteFallback(GuiGraphics g, int bx1, int by1, int boxSize, int cx, String name, int dexNumber) {
        ResourceLocation sprite = PokemonSpriteCache.get(dexNumber, false);
        if (sprite != null) {
            var pose = g.pose();
            pose.pushPose();
            float scale = boxSize / 96f;
            pose.translate(bx1, by1, 0f);
            pose.scale(scale, scale, 1f);
            g.blit(sprite, 0, 0, 0, 0, 96, 96, 96, 96);
            pose.popPose();
        } else {
            int rc = rarityStrColor(getRarityLabel(name));
            g.fill(bx1, by1, bx1 + boxSize, by1 + boxSize,
                    new Color((rc>>16)&0xFF, (rc>>8)&0xFF, rc&0xFF, 35).getRGB());
            g.drawCenteredString(minecraft.font, "?", cx, by1 + boxSize/2 - 4, 0x445566);
        }
    }

    // ── Entity building ───────────────────────────────────────────────────────

    private static final ResourceLocation POKEMON_TYPE =
            ResourceLocation.fromNamespaceAndPath("cobblemon", "pokemon");

    private LivingEntity getOrBuildEntity(String name) {
        LivingEntity world = findInWorld(name);
        if (world != null) return world;
        if (entityCache.containsKey(name)) return entityCache.get(name);
        LivingEntity built = buildViaReflection(name);
        entityCache.put(name, built);
        return built;
    }

    private LivingEntity findInWorld(String target) {
        if (minecraft.level == null) return null;
        try {
            for (Entity e : minecraft.level.entitiesForRendering()) {
                if (!POKEMON_TYPE.equals(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()))) continue;
                Object pok = e.getClass().getMethod("getPokemon").invoke(e);
                if (pok == null) continue;
                Object sp = pok.getClass().getMethod("getSpecies").invoke(pok);
                String n  = (String) sp.getClass().getMethod("getName").invoke(sp);
                if (target.equalsIgnoreCase(n) && e instanceof LivingEntity le) return le;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private LivingEntity buildViaReflection(String name) {
        if (minecraft.level == null) return null;
        try {
            Class<?> pokClass = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon");
            Object pokemon = pokClass.getDeclaredConstructor().newInstance();

            Object speciesReg = Class.forName("com.cobblemon.mod.common.api.pokemon.PokemonSpecies")
                    .getField("INSTANCE").get(null);
            Object species = null;
            for (java.lang.reflect.Method m : speciesReg.getClass().getMethods()) {
                if (m.getName().equals("getByName") && m.getParameterCount() == 1) {
                    species = m.invoke(speciesReg, name.toLowerCase()); break;
                }
            }
            if (species == null) return null;
            for (java.lang.reflect.Method s : pokClass.getMethods()) {
                if (s.getName().equals("setSpecies") && s.getParameterCount() == 1) {
                    s.invoke(pokemon, species); break;
                }
            }

            Class<?> entClass = Class.forName("com.cobblemon.mod.common.entity.pokemon.PokemonEntity");
            var type = BuiltInRegistries.ENTITY_TYPE.get(POKEMON_TYPE);
            if (type == null) return null;

            Object entity = null;
            // Try (EntityType, Level) — standard MC
            for (java.lang.reflect.Constructor<?> c : entClass.getDeclaredConstructors()) {
                Class<?>[] p = c.getParameterTypes();
                if (p.length == 2 && p[1].isAssignableFrom(minecraft.level.getClass())) {
                    entity = c.newInstance(type, minecraft.level); break;
                }
            }
            // Try (Level, Pokemon) — Cobblemon convenience
            if (entity == null) {
                for (java.lang.reflect.Constructor<?> c : entClass.getDeclaredConstructors()) {
                    Class<?>[] p = c.getParameterTypes();
                    if (p.length == 2 && p[0].isAssignableFrom(minecraft.level.getClass())) {
                        entity = c.newInstance(minecraft.level, pokemon); break;
                    }
                }
            }
            if (entity != null) {
                for (java.lang.reflect.Method m : entClass.getMethods()) {
                    if (m.getName().equals("setPokemon") && m.getParameterCount() == 1) {
                        m.invoke(entity, pokemon); break;
                    }
                }
                if (entity instanceof LivingEntity le) return le;
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int x = (int) mx, y = (int) my;

        // Close
        int closeX = px + pw - 16, closeY = py + 4;
        if (x >= closeX && x < closeX+12 && y >= closeY && y < closeY+12) {
            minecraft.setScreen(null); return true;
        }

        // Range bar
        int barX = settingsX + 38, barY = py + 39;
        int numW = minecraft.font.width("256m") + 2;
        int barW = settingsW - 38 - numW - 6;
        if (x >= barX && x <= barX+barW && y >= barY && y <= barY+9) {
            draggingRange = true;
            module.range.setValue(Math.max(1, Math.min(256,
                    (int)((x - barX) / (float) barW * 256))));
            return true;
        }

        // Min Rarity
        int rarY = barY + 14;
        int btnX = settingsX + 60;
        int btnW = minecraft.font.width(module.minRarity.getValue().name()) + 10;
        if (x >= btnX && x <= btnX+btnW && y >= rarY-1 && y <= rarY+10) {
            CobblemonSpawnModule.Rarity[] vals = CobblemonSpawnModule.Rarity.values();
            module.minRarity.setValue(vals[(module.minRarity.getValue().ordinal()+1) % vals.length]);
            return true;
        }

        // Checkboxes
        int cbY1 = rarY + 14, cbY2 = cbY1 + 13, cbY3 = cbY2 + 13, cbY4 = cbY3 + 13;
        if (x >= settingsX && x <= settingsX+10 && y >= cbY1 && y <= cbY1+10)
            { module.shinies.setValue(!module.shinies.getValue()); return true; }
        if (x >= settingsX && x <= settingsX+10 && y >= cbY2 && y <= cbY2+10)
            { module.esp.setValue(!module.esp.getValue()); return true; }
        if (x >= settingsX && x <= settingsX+10 && y >= cbY3 && y <= cbY3+10)
            { module.sound.setValue(!module.sound.getValue()); return true; }
        if (x >= settingsX && x <= settingsX+10 && y >= cbY4 && y <= cbY4+10)
            { module.huntDebug.setValue(!module.huntDebug.getValue()); return true; }

        // Particle controls
        // Species list
        int listX = filterX-1, listY = py + 55;
        int listW = filterW+2, listH = LIST_ROWS * ROW_H;
        if (x >= listX && x <= listX+listW && y >= listY && y < listY+listH && btn == 0) {
            int row = (y - listY) / ROW_H + listScrollOffset;
            if (row < visible.size()) {
                previewName = visible.get(row).name();
                toggleFilter(previewName);
                return true;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (draggingRange && btn == 0) {
            int barX = settingsX + 38;
            int numW = minecraft.font.width("256m") + 2;
            int barW = settingsW - 38 - numW - 6;
            float pct = ((float) mx - barX) / barW;
            module.range.setValue(Math.max(1, Math.min(256, (int)(pct * 256))));
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        draggingRange = false;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        int listX = filterX-1, listY = py + 55;
        int listW = filterW+2, listH = LIST_ROWS * ROW_H;
        if (mx >= listX && mx <= listX+listW && my >= listY && my < listY+listH) {
            listScrollOffset = Math.max(0, Math.min(
                    listScrollOffset - (int) vScroll,
                    Math.max(0, visible.size() - LIST_ROWS)));
            return true;
        }
        return super.mouseScrolled(mx, my, hScroll, vScroll);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) { minecraft.setScreen(null); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override public boolean isPauseScreen() { return false; }

    // ── Drawing helpers ───────────────────────────────────────────────────────

    private int drawCheckbox(GuiGraphics g, int x, int y, String label, boolean state, int mx, int my) {
        boolean hov = mx >= x && mx <= x+10 && my >= y && my <= y+10;
        g.fill(x, y, x+10, y+10, state ? new Color(65, 48, 120).getRGB() : new Color(28, 26, 44).getRGB());
        drawBorder(g, x, y, 10, 10, hov ? new Color(88, 68, 145).getRGB() : new Color(55, 50, 80).getRGB());
        if (state) g.drawString(minecraft.font, "✓", x+1, y+1, 0xFFFFFF);
        g.drawString(minecraft.font, label, x+13, y+1, 0xAAAAAA);
        return y + 13;
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x,       y,       x+w, y+1,   color);
        g.fill(x,       y+h-1,   x+w, y+h,   color);
        g.fill(x,       y,       x+1, y+h,    color);
        g.fill(x+w-1,   y,       x+w, y+h,   color);
    }

    private void toggleFilter(String name) {
        String cur = module.filter.getValue();
        List<String> list = new ArrayList<>(Arrays.stream(cur.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList());
        String norm = name.toLowerCase();
        if (list.stream().anyMatch(s -> s.equalsIgnoreCase(norm))) list.removeIf(s -> s.equalsIgnoreCase(norm));
        else list.add(norm);
        module.filter.setValue(String.join(",", list));
    }

    private boolean isInFilter(String name) {
        String cur = module.filter.getValue();
        if (cur.isBlank()) return false;
        return Arrays.stream(cur.split(",")).map(String::trim).anyMatch(s -> s.equalsIgnoreCase(name));
    }

    public static String getRarityLabel(String name) {
        String n  = name.toLowerCase().replace(" ","").replace("-","");
        String nd = name.toLowerCase().replace(" ","");
        if (CobblemonSpawnModule.LEGENDARIES.contains(n)       || CobblemonSpawnModule.LEGENDARIES.contains(nd))       return "ultra-rare";
        if (CobblemonSpawnModule.PSEUDO_LEGENDARIES.contains(n)|| CobblemonSpawnModule.PSEUDO_LEGENDARIES.contains(nd)) return "rare";
        if (CobblemonSpawnModule.UNCOMMONS.contains(n)         || CobblemonSpawnModule.UNCOMMONS.contains(nd))          return "uncommon";
        return "common";
    }

    private static int rarityEnumColor(CobblemonSpawnModule.Rarity r) {
        return switch (r) {
            case ALL        -> 0xAAAAAA;
            case UNCOMMON   -> 0x55FF55;
            case RARE       -> 0x55FFFF;
            case ULTRA_RARE -> 0xFF55FF;
        };
    }

    private static int rarityStrColor(String r) {
        return switch (r) {
            case "ultra-rare" -> 0xFF55FF;
            case "rare"       -> 0x55FFFF;
            case "uncommon"   -> 0x55FF55;
            default           -> 0x777777;
        };
    }

    private static String rarityBadge(String r) {
        return switch (r) {
            case "ultra-rare" -> "UR";
            case "rare"       -> "R";
            case "uncommon"   -> "UC";
            default           -> "";
        };
    }
}
