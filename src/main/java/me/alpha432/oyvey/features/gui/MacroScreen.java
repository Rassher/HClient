package me.alpha432.oyvey.features.gui;

import me.alpha432.oyvey.features.modules.client.MacroModule;
import me.alpha432.oyvey.features.modules.client.MacroModule.MacroEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class MacroScreen extends Screen {

    private final MacroModule module;

    // Panel geometry
    private int px, py, pw, ph;

    // Particle system
    private static final int   PARTICLE_COUNT = 40;
    private static final float CONNECT_DIST   = 90f;
    private static final float BASE_SPEED     = 0.022f;
    private static class Particle { float x, y, vx, vy; }
    private final List<Particle> particles = new ArrayList<>();
    private float rainbowHue = 0f;
    private long  lastFrameNs = System.nanoTime();

    // Row layout
    private static final int ROW_H    = 22;
    private static final int KEY_W    = 58;
    private static final int DEL_W    = 14;
    private static final int ROW_PAD  = 6;
    private static final int VISIBLE_ROWS = 9;

    // Per-row EditBoxes (command inputs)
    private final List<EditBox> cmdBoxes = new ArrayList<>();

    // State
    private int listeningRow = -1; // which row is waiting for a key press
    private int scrollOffset = 0;

    public MacroScreen(MacroModule module) {
        super(Component.literal("Macros"));
        this.module = module;
    }

    @Override
    protected void init() {
        pw = Math.min(width - 40, 520);
        ph = Math.min(height - 40, 380);
        px = (width  - pw) / 2;
        py = (height - ph) / 2;

        particles.clear();
        var rnd = new java.util.Random();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            Particle p = new Particle();
            p.x  = px + 2 + rnd.nextFloat() * (pw - 4);
            p.y  = py + 22 + rnd.nextFloat() * (ph - 24);
            double ang = rnd.nextDouble() * Math.PI * 2;
            float spd = BASE_SPEED * (0.5f + rnd.nextFloat());
            p.vx = (float) Math.cos(ang) * spd;
            p.vy = (float) Math.sin(ang) * spd;
            particles.add(p);
        }
        rebuildBoxes();
    }

    // ── Row edit boxes ────────────────────────────────────────────────────────

    private void rebuildBoxes() {
        cmdBoxes.forEach(this::removeWidget);
        cmdBoxes.clear();

        int cmdW = pw - KEY_W - DEL_W - ROW_PAD * 4 - 2;
        for (int i = 0; i < module.entries.size(); i++) {
            MacroEntry entry = module.entries.get(i);
            int ry = rowY(i);
            int bx = px + ROW_PAD + KEY_W + ROW_PAD;
            EditBox box = new EditBox(minecraft.font, bx, ry + 4, cmdW, 14,
                    Component.literal("Command"));
            box.setMaxLength(256);
            box.setBordered(false);
            box.setValue(entry.command);
            final int idx = i;
            box.setResponder(text -> {
                if (idx < module.entries.size()) {
                    module.entries.get(idx).command = text;
                    module.serialize();
                }
            });
            addWidget(box);
            cmdBoxes.add(box);
        }
    }

    private int rowY(int i) {
        int contentTop = py + 36;
        return contentTop + (i - scrollOffset) * ROW_H;
    }

    private int contentBottom() { return py + ph - 30; }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0x88000000);
        g.fill(px + 4, py + 4, px + pw + 4, py + ph + 4, 0x55000000);
        g.fill(px, py, px + pw, py + ph, new Color(10, 8, 16, 255).getRGB());

        g.fill(px, py, px + pw, py + ph, 0xE0080610);
        updateAndDrawParticles(g);

        // Title bar
        g.fill(px, py, px + pw, py + 20, new Color(18, 14, 30, 245).getRGB());
        g.fill(px, py + 20, px + pw, py + 21, new Color(70, 50, 110).getRGB());
        g.drawString(minecraft.font, "✦ Macros", px + 8, py + 6, 0xCCAAFF);

        // Close button
        int csz = 12, cx2 = px + pw - csz - 4, cy2 = py + (20 - csz) / 2;
        boolean hClose = mouseX >= cx2 && mouseX < cx2 + csz && mouseY >= cy2 && mouseY < cy2 + csz;
        g.fill(cx2, cy2, cx2 + csz, cy2 + csz, hClose ? new Color(180, 50, 50, 220).getRGB() : new Color(55, 45, 75).getRGB());
        drawBorder(g, cx2, cy2, csz, csz, hClose ? 0xFFCC4444 : new Color(80, 65, 105).getRGB());
        g.drawString(minecraft.font, "✕", cx2 + (csz - minecraft.font.width("✕")) / 2,
                cy2 + (csz - minecraft.font.lineHeight) / 2 + 1, hClose ? 0xFFFFFF : 0xCCCCCC, false);

        // Column headers
        int hdrY = py + 24;
        g.drawString(minecraft.font, "KEY", px + ROW_PAD + (KEY_W - minecraft.font.width("KEY")) / 2, hdrY, 0x9988BB);
        g.drawString(minecraft.font, "COMMAND", px + ROW_PAD + KEY_W + ROW_PAD, hdrY, 0x9988BB);
        g.fill(px, py + 33, px + pw, py + 34, new Color(45, 40, 65).getRGB());

        // Rows
        g.enableScissor(px + 1, py + 34, px + pw - 1, contentBottom());
        renderRows(g, mouseX, mouseY);
        g.disableScissor();

        // Add button
        renderAddButton(g, mouseX, mouseY);

        // Border
        drawBorder(g, px, py, pw, ph, new Color(55, 45, 80).getRGB());

        super.render(g, mouseX, mouseY, delta);
    }

    private void renderRows(GuiGraphics g, int mouseX, int mouseY) {
        int cmdW = pw - KEY_W - DEL_W - ROW_PAD * 4 - 2;
        List<MacroEntry> entries = module.entries;
        int maxRow = Math.min(entries.size(), scrollOffset + VISIBLE_ROWS);

        for (int i = scrollOffset; i < maxRow; i++) {
            MacroEntry entry = entries.get(i);
            int ry = rowY(i);
            if (ry + ROW_H > contentBottom()) break;

            boolean rowHov = mouseX >= px + 1 && mouseX < px + pw - 1 && mouseY >= ry && mouseY < ry + ROW_H;
            if (rowHov) g.fill(px + 1, ry, px + pw - 1, ry + ROW_H, new Color(30, 24, 50, 180).getRGB());

            // Key button
            boolean listening = listeningRow == i;
            int kx = px + ROW_PAD, ky = ry + 3;
            int kBg = listening ? new Color(100, 30, 140, 230).getRGB() : new Color(35, 30, 55, 220).getRGB();
            g.fill(kx, ky, kx + KEY_W, ky + ROW_H - 6, kBg);
            drawBorder(g, kx, ky, KEY_W, ROW_H - 6, listening ? 0xFFCC88FF : new Color(68, 58, 98).getRGB());
            String keyLabel = listening ? "..." : MacroModule.keyName(entry.key);
            int klColor = listening ? 0xFFCC88FF : (entry.key < 0 ? 0x666677 : 0xCCAAFF);
            g.drawString(minecraft.font, keyLabel, kx + (KEY_W - minecraft.font.width(keyLabel)) / 2,
                    ky + (ROW_H - 6 - minecraft.font.lineHeight) / 2 + 1, klColor, false);

            // Command box background
            int bx = kx + KEY_W + ROW_PAD;
            g.fill(bx, ky, bx + cmdW, ky + ROW_H - 6, new Color(25, 22, 40, 220).getRGB());
            drawBorder(g, bx, ky, cmdW, ROW_H - 6, new Color(55, 50, 80).getRGB());

            // Render EditBox (if in range)
            if (i < cmdBoxes.size()) {
                EditBox box = cmdBoxes.get(i);
                box.setX(bx + 2); box.setY(ky + 3); box.setWidth(cmdW - 4);
                box.render(g, mouseX, mouseY, 0);
            }

            // Delete button
            int dx = bx + cmdW + ROW_PAD - 2, dy = ky;
            boolean hDel = mouseX >= dx && mouseX < dx + DEL_W && mouseY >= dy && mouseY < dy + ROW_H - 6;
            g.fill(dx, dy, dx + DEL_W, dy + ROW_H - 6, hDel ? new Color(160, 40, 40, 220).getRGB() : new Color(55, 30, 55, 200).getRGB());
            drawBorder(g, dx, dy, DEL_W, ROW_H - 6, hDel ? 0xFFCC4444 : new Color(80, 50, 80).getRGB());
            g.drawString(minecraft.font, "✕", dx + (DEL_W - minecraft.font.width("✕")) / 2,
                    dy + (ROW_H - 6 - minecraft.font.lineHeight) / 2 + 1, hDel ? 0xFFFFFF : 0xCC8888, false);

            // Row separator
            g.fill(px + 4, ry + ROW_H - 1, px + pw - 4, ry + ROW_H, new Color(35, 30, 55).getRGB());
        }
    }

    private void renderAddButton(GuiGraphics g, int mouseX, int mouseY) {
        int bw = 80, bh = 14;
        int bx = px + (pw - bw) / 2;
        int by = contentBottom() + 6;
        boolean hov = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
        g.fill(bx, by, bx + bw, by + bh, hov ? new Color(65, 50, 110, 230).getRGB() : new Color(40, 35, 68, 220).getRGB());
        drawBorder(g, bx, by, bw, bh, hov ? new Color(100, 75, 160).getRGB() : new Color(65, 55, 95).getRGB());
        String label = "+ Add Macro";
        g.drawString(minecraft.font, label, bx + (bw - minecraft.font.width(label)) / 2, by + 3, hov ? 0xCCAAFF : 0x9988BB, false);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int x = (int) mx, y = (int) my;

        // Close
        int csz = 12, cx2 = px + pw - csz - 4, cy2 = py + (20 - csz) / 2;
        if (x >= cx2 && x < cx2 + csz && y >= cy2 && y < cy2 + csz) {
            minecraft.setScreen(null); return true;
        }

        // Add button
        int bw = 80, bh = 14;
        int abx = px + (pw - bw) / 2, aby = contentBottom() + 6;
        if (x >= abx && x < abx + bw && y >= aby && y < aby + bh) {
            module.entries.add(new MacroEntry());
            module.serialize();
            scrollOffset = Math.max(0, module.entries.size() - VISIBLE_ROWS);
            rebuildBoxes();
            return true;
        }

        // Row interactions
        int maxRow = Math.min(module.entries.size(), scrollOffset + VISIBLE_ROWS);
        int cmdW = pw - KEY_W - DEL_W - ROW_PAD * 4 - 2;
        for (int i = scrollOffset; i < maxRow; i++) {
            int ry = rowY(i);
            if (ry + ROW_H > contentBottom()) break;
            int ky = ry + 3;

            // Key button
            int kx = px + ROW_PAD;
            if (x >= kx && x < kx + KEY_W && y >= ky && y < ky + ROW_H - 6) {
                listeningRow = (listeningRow == i) ? -1 : i;
                return true;
            }

            // Delete button
            int bx2 = kx + KEY_W + ROW_PAD + cmdW + ROW_PAD - 2;
            if (x >= bx2 && x < bx2 + DEL_W && y >= ky && y < ky + ROW_H - 6) {
                listeningRow = -1;
                module.entries.remove(i);
                module.serialize();
                rebuildBoxes();
                return true;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int mods) {
        if (listeningRow >= 0 && listeningRow < module.entries.size()) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                listeningRow = -1;
            } else {
                module.entries.get(listeningRow).key = keyCode;
                module.serialize();
                listeningRow = -1;
            }
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            minecraft.setScreen(null); return true;
        }
        return super.keyPressed(keyCode, scanCode, mods);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        int maxScroll = Math.max(0, module.entries.size() - VISIBLE_ROWS);
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int) v, maxScroll));
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }

    // ── Particle system ───────────────────────────────────────────────────────

    private void updateAndDrawParticles(GuiGraphics g) {
        long now = System.nanoTime();
        float dt = Math.min((now - lastFrameNs) / 1_000_000f, 50f);
        lastFrameNs = now;
        rainbowHue = (rainbowHue + 0.35f * dt / 16.67f) % 360f;

        float minX = px + 2, maxX = px + pw - 2;
        float minY = py + 22, maxY = py + ph - 2;
        for (Particle p : particles) {
            p.x += p.vx * dt * 5f;
            p.y += p.vy * dt * 5f;
            if (p.x < minX) { p.x = minX; p.vx =  Math.abs(p.vx); }
            if (p.x > maxX) { p.x = maxX; p.vx = -Math.abs(p.vx); }
            if (p.y < minY) { p.y = minY; p.vy =  Math.abs(p.vy); }
            if (p.y > maxY) { p.y = maxY; p.vy = -Math.abs(p.vy); }
        }

        g.enableScissor((int)minX, (int)minY, (int)maxX, (int)maxY);

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
                    drawLineWithFill(g, a.x, a.y, b.x, b.y, hslToArgb(h, 1f, 0.65f, alpha));
                    drawn++;
                }
            }
        }
        for (Particle p : particles) {
            float h = (rainbowHue + ((p.x - px) / pw) * 80f) % 360f;
            g.fill((int)p.x - 1, (int)p.y - 1, (int)p.x + 2, (int)p.y + 2, hslToArgb(h, 1f, 0.75f, 0.90f));
        }

        g.disableScissor();
    }

    private static void drawLineWithFill(GuiGraphics g, float x1, float y1, float x2, float y2, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        int steps = Math.max(1, (int)(Math.sqrt(dx*dx + dy*dy) / 2f));
        for (int s = 0; s <= steps; s++) {
            float t = s / (float) steps;
            int lx = (int)(x1 + dx * t), ly = (int)(y1 + dy * t);
            g.fill(lx, ly, lx + 1, ly + 1, color);
        }
    }

    private static int hslToArgb(float h, float s, float l, float alpha) {
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs((h / 60f) % 2 - 1));
        float m = l - c / 2;
        float r, g, b;
        if      (h < 60)  { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else              { r = c; g = 0; b = x; }
        int ri = (int)((r + m) * 255), gi = (int)((g + m) * 255), bi = (int)((b + m) * 255);
        int ai = (int)(alpha * 255);
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    // ── Drawing helpers ───────────────────────────────────────────────────────

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x,     y,     x+w, y+1,   color);
        g.fill(x,     y+h-1, x+w, y+h,   color);
        g.fill(x,     y,     x+1, y+h,   color);
        g.fill(x+w-1, y,     x+w, y+h,   color);
    }
}
