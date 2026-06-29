package me.alpha432.oyvey.features.gui.title;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticleSystem {

    private static final int   COUNT           = 55;
    private static final float CONNECTION_DIST = 120f;
    private static final float SPEED           = 1.2f;
    private static final int   DOT_RADIUS      = 3;
    private static final int   MAX_LINES       = 120;
    private static final float HUE_SPEED       = 2.5f; // faster rainbow

    private final List<Particle> particles = new ArrayList<>();
    private float hue = 0f;
    private int screenW, screenH;
    private final Random rng = new Random();

    public void init(int w, int h) {
        screenW = w;
        screenH = h;
        particles.clear();
        for (int i = 0; i < COUNT; i++) particles.add(spawn());
    }

    public void resize(int w, int h) {
        screenW = w;
        screenH = h;
    }

    public void tick(float delta) {
        hue = (hue + HUE_SPEED * delta) % 360f;
        for (Particle p : particles) {
            p.x += p.vx * delta;
            p.y += p.vy * delta;
            if (p.x - DOT_RADIUS < 0)      { p.x = DOT_RADIUS;           p.vx = -p.vx; }
            if (p.x + DOT_RADIUS > screenW) { p.x = screenW - DOT_RADIUS; p.vx = -p.vx; }
            if (p.y - DOT_RADIUS < 0)      { p.y = DOT_RADIUS;           p.vy = -p.vy; }
            if (p.y + DOT_RADIUS > screenH) { p.y = screenH - DOT_RADIUS; p.vy = -p.vy; }
        }
    }

    public void render(GuiGraphics graphics) {
        // --- Lines — capped at MAX_LINES to keep framerate stable ---
        int drawn = 0;
        outer:
        for (int i = 0; i < particles.size(); i++) {
            Particle a = particles.get(i);
            for (int j = i + 1; j < particles.size(); j++) {
                if (drawn >= MAX_LINES) break outer;
                Particle b = particles.get(j);
                float dx = a.x - b.x, dy = a.y - b.y;
                float d  = Mth.sqrt(dx * dx + dy * dy);
                if (d < CONNECTION_DIST) {
                    float alpha = (1f - d / CONNECTION_DIST) * 0.75f;
                    float h     = (hue + (a.x + b.x) * 0.5f / screenW * 80f) % 360f;
                    int   color = hslToArgb(h, 1f, 0.65f, alpha);
                    drawLineWithFill(graphics, a.x, a.y, b.x, b.y, color);
                    drawn++;
                }
            }
        }

        // --- Dots (graphics.fill is always reliable) ---
        for (Particle p : particles) {
            float wave  = (p.x / screenW) * 80f;
            int   color = hslToArgb((hue + wave) % 360f, 1f, 0.6f, 1f);
            graphics.fill((int) p.x - DOT_RADIUS, (int) p.y - DOT_RADIUS,
                          (int) p.x + DOT_RADIUS, (int) p.y + DOT_RADIUS, color);
        }
    }

    // -----------------------------------------------------------------------

    private Particle spawn() {
        Particle p  = new Particle();
        p.x         = rng.nextFloat() * screenW;
        p.y         = rng.nextFloat() * screenH;
        float angle = rng.nextFloat() * Mth.TWO_PI;
        float speed = (rng.nextFloat() * 0.7f + 0.3f) * SPEED;
        p.vx        = Mth.cos(angle) * speed;
        p.vy        = Mth.sin(angle) * speed;
        return p;
    }

    private static void drawLineWithFill(GuiGraphics g, float x1, float y1, float x2, float y2, int color) {
        float dx    = x2 - x1, dy = y2 - y1;
        float len   = Mth.sqrt(dx * dx + dy * dy);
        int   steps = Math.max(1, (int) (len / 2f));
        for (int s = 0; s <= steps; s++) {
            float t  = s / (float) steps;
            int   lx = (int) (x1 + dx * t);
            int   ly = (int) (y1 + dy * t);
            g.fill(lx, ly, lx + 1, ly + 1, color);
        }
    }

    private static int hslToArgb(float h, float s, float l, float a) {
        float c  = (1f - Math.abs(2f * l - 1f)) * s;
        float x  = c * (1f - Math.abs((h / 60f) % 2f - 1f));
        float m  = l - c / 2f;
        float r, g, b;
        if      (h < 60)  { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else              { r = c; g = 0; b = x; }
        int ri = (int) ((r + m) * 255), gi = (int) ((g + m) * 255),
            bi = (int) ((b + m) * 255), ai = (int) (a * 255);
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    private static class Particle {
        float x, y, vx, vy;
    }
}
