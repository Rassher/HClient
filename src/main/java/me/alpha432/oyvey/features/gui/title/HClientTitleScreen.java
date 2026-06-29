package me.alpha432.oyvey.features.gui.title;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.loader.api.FabricLoader;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class HClientTitleScreen extends TitleScreen {

    private final ParticleSystem particles = new ParticleSystem();
    private PanoramaRenderer customRenderer;

    // Title textures
    private ResourceLocation titleBorderTex;
    private ResourceLocation titleFillLoc;
    private DynamicTexture   titleFillDynTex;
    private NativeImage      titleFillNative;
    private int[]            titleFillAlpha;
    private int titleTexW, titleTexH;

    private float waveOffset = 0f;
    private static final float WAVE_SPEED = 10f;
    private static final float WAVE_RANGE = 200f;

    // Vanilla button references for particles mode
    private AbstractButton spBtn, mpBtn, optsBtn, modsBtn, quitBtn;

    // Custom button layout
    private static final int BTN_GAP  = 10;
    private static final int ROW_GAP  = 8;
    private static final int QUIT_SZ  = 22;
    private int btnW, mainBtnH, shortH, bX1, bX2, sqY, shortY;

    public HClientTitleScreen() { super(); }

    @Override
    protected void init() {
        super.init();
        // Remove vanilla copyright widget (bottom-right)
        this.renderables.removeIf(r ->
            r instanceof net.minecraft.client.gui.components.AbstractWidget w
            && w.getX() > width / 2 && w.getY() > height - 20);

        // Find vanilla buttons by translated text
        spBtn = mpBtn = optsBtn = modsBtn = quitBtn = null;
        for (var r : this.renderables) {
            if (r instanceof AbstractButton b) {
                String msg = b.getMessage().getString().toLowerCase();
                if      ((msg.contains("jugador") || msg.contains("single")) && !msg.contains("multi")) spBtn   = b;
                else if (msg.contains("multi"))                                                          mpBtn   = b;
                else if (msg.contains("opci") || msg.contains("option"))                                optsBtn = b;
                else if (msg.contains("cerrar") || msg.contains("quit") || msg.contains("salir"))       quitBtn = b;
                else if (msg.contains("mod"))                                                            modsBtn = b;
            }
        }

        PanoramaManager.INSTANCE.init();
        particles.init(width, height);
        rebuildRenderer();
        if (titleBorderTex == null) buildTitleTextures();
        computeLayout();
    }

    private void computeLayout() {
        btnW     = Math.min(140, (width - 60) / 2 - BTN_GAP);
        mainBtnH = 40;
        shortH   = 24;
        int totalW = btnW * 2 + BTN_GAP;
        bX1    = width / 2 - totalW / 2;
        bX2    = bX1 + btnW + BTN_GAP;
        int totalH = mainBtnH + ROW_GAP + shortH;
        sqY    = height / 2 - totalH / 2 + 10;
        shortY = sqY + mainBtnH + ROW_GAP;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        PanoramaManager.Background bg = PanoramaManager.INSTANCE.getCurrent();
        boolean isParticles = bg.type == PanoramaManager.BackgroundType.PARTICLES;

        // 1. Background
        if (isParticles) {
            g.fill(0, 0, width, height, 0xFF141414);
            particles.tick(delta);
            particles.render(g);
        } else {
            RenderSystem.enableBlend();
            customRenderer.render(g, width, height, 1.0f, delta);
            RenderSystem.disableBlend();
            g.fill(0, 0, width, height, 0x73000000);
        }

        // 2. Title
        renderTitle(g, delta);

        // 3. Buttons
        if (isParticles) {
            renderParticleButtons(g, mouseX, mouseY);
        } else {
            for (Renderable r : this.renderables) r.render(g, mouseX, mouseY, delta);
        }

        // 4. Switcher + version
        renderSwitcher(g, mouseX, mouseY);
        renderVersionInfo(g);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && handleSwitcherClick(mouseX, mouseY)) return true;
        if (PanoramaManager.INSTANCE.getCurrent().type == PanoramaManager.BackgroundType.PARTICLES) {
            return handleParticleButtonClick(mouseX, mouseY);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void resize(Minecraft mc, int w, int h) {
        super.resize(mc, w, h);
        particles.resize(w, h);
        computeLayout();
    }

    // -----------------------------------------------------------------------
    // Custom particle-mode buttons

    private void renderParticleButtons(GuiGraphics g, int mouseX, int mouseY) {
        int qx = width - 8 - QUIT_SZ;

        // Quit X — top-right: same bg as other buttons, red only on hover
        boolean hovQ = isIn(mouseX, mouseY, qx, 8, QUIT_SZ, QUIT_SZ);
        drawRoundBtn(g, qx, 8, QUIT_SZ, QUIT_SZ, hovQ ? 0xCC3D0A0A : 0xCC0D0D0D, hovQ ? 0xFF884444 : 0xFF444444);
        g.drawCenteredString(font, "✕", qx + QUIT_SZ / 2, 8 + QUIT_SZ / 2 - font.lineHeight / 2,
                             hovQ ? 0xFFFF8888 : 0xFFAAAAAA);

        // Singleplayer — rect left
        boolean hovSP = isIn(mouseX, mouseY, bX1, sqY, btnW, mainBtnH);
        drawRoundBtn(g, bX1, sqY, btnW, mainBtnH, hovSP ? 0xCC222222 : 0xCC0D0D0D, hovSP ? 0xFF888888 : 0xFF444444);
        drawBtnLabel(g, spBtn != null ? spBtn.getMessage().getString() : "Un jugador",
                     bX1, sqY, btnW, mainBtnH);

        // Multiplayer — rect right
        boolean hovMP = isIn(mouseX, mouseY, bX2, sqY, btnW, mainBtnH);
        drawRoundBtn(g, bX2, sqY, btnW, mainBtnH, hovMP ? 0xCC222222 : 0xCC0D0D0D, hovMP ? 0xFF888888 : 0xFF444444);
        drawBtnLabel(g, mpBtn != null ? mpBtn.getMessage().getString() : "Multijugador",
                     bX2, sqY, btnW, mainBtnH);

        // Options — short left
        boolean hovOp = isIn(mouseX, mouseY, bX1, shortY, btnW, shortH);
        drawRoundBtn(g, bX1, shortY, btnW, shortH, hovOp ? 0xCC222222 : 0xCC0D0D0D, hovOp ? 0xFF888888 : 0xFF444444);
        String optsLabel = optsBtn != null ? optsBtn.getMessage().getString() : "Opciones";
        optsLabel = optsLabel.replace("...", "").replace("…", "").trim();
        drawBtnLabel(g, optsLabel, bX1, shortY, btnW, shortH);

        // Mods — short right
        boolean hovMd = isIn(mouseX, mouseY, bX2, shortY, btnW, shortH);
        drawRoundBtn(g, bX2, shortY, btnW, shortH, hovMd ? 0xCC222222 : 0xCC0D0D0D, hovMd ? 0xFF888888 : 0xFF444444);
        drawBtnLabel(g, modsBtn != null ? modsBtn.getMessage().getString() : "Mods",
                     bX2, shortY, btnW, shortH);
    }

    private boolean handleParticleButtonClick(double mx, double my) {
        int qx = width - 8 - QUIT_SZ;
        if (isIn(mx, my, qx, 8, QUIT_SZ, QUIT_SZ)) {
            if (quitBtn != null) quitBtn.onPress(); else minecraft.stop();
            return true;
        }
        if (isIn(mx, my, bX1, sqY, btnW, mainBtnH)) { if (spBtn != null) spBtn.onPress(); return true; }
        if (isIn(mx, my, bX2, sqY, btnW, mainBtnH)) {
            minecraft.setScreen(new me.alpha432.oyvey.features.gui.servers.HClientServerListScreen(this));
            return true;
        }
        if (isIn(mx, my, bX1, shortY, btnW, shortH)) { if (optsBtn != null) optsBtn.onPress(); return true; }
        if (isIn(mx, my, bX2, shortY, btnW, shortH)) { if (modsBtn != null) modsBtn.onPress(); return true; }
        return false;
    }

    /** Draws a button with 2px clipped corners + 2px border for a cleaner rounded look. */
    private void drawRoundBtn(GuiGraphics g, int x, int y, int w, int h, int bg, int border) {
        // Fill — clip 2px corners
        g.fill(x + 2, y,         x + w - 2, y + h,         bg);
        g.fill(x,     y + 2,     x + 2,     y + h - 2,     bg);
        g.fill(x + w - 2, y + 2, x + w,     y + h - 2,     bg);
        // Inner corner anti-alias (1px diagonal fill)
        g.fill(x + 1, y + 1,     x + 2,     y + 2,         bg);
        g.fill(x + w - 2, y + 1, x + w - 1, y + 2,         bg);
        g.fill(x + 1, y + h - 2, x + 2,     y + h - 1,     bg);
        g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, bg);

        // Border — 2px thick, skip 2px corners
        // Top
        g.fill(x + 2, y,     x + w - 2, y + 1,     border);
        g.fill(x + 2, y + 1, x + w - 2, y + 2,     border);
        // Bottom
        g.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, border);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h,     border);
        // Left
        g.fill(x,     y + 2, x + 1,     y + h - 2, border);
        g.fill(x + 1, y + 2, x + 2,     y + h - 2, border);
        // Right
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, border);
        g.fill(x + w - 1, y + 2, x + w,     y + h - 2, border);
        // Diagonal corner border pixels
        g.fill(x + 1,     y + 1,     x + 2,     y + 2,     border);
        g.fill(x + w - 2, y + 1,     x + w - 1, y + 2,     border);
        g.fill(x + 1,     y + h - 2, x + 2,     y + h - 1, border);
        g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, border);
    }

    private void drawBtnLabel(GuiGraphics g, String text, int x, int y, int w, int h) {
        int cx = x + w / 2;
        int cy = y + h / 2 - font.lineHeight / 2;
        g.drawCenteredString(font, text, cx, cy, 0xFFFFFFFF);
    }

    private static boolean isIn(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    // -----------------------------------------------------------------------
    // Title texture

    private void buildTitleTextures() {
        try {
            InputStream stream = getClass().getResourceAsStream("/assets/oyvey/font/title.ttf");
            if (stream == null) return;

            java.awt.Font awtFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, stream)
                .deriveFont(java.awt.Font.PLAIN, 62f);
            stream.close();

            BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D tg = tmp.createGraphics();
            FontRenderContext frc = new FontRenderContext(null, true, true);
            GlyphVector gv = awtFont.createGlyphVector(frc, "HClient");
            java.awt.geom.Rectangle2D bounds = gv.getVisualBounds();
            FontMetrics fm = tg.getFontMetrics(awtFont);
            tg.dispose();

            int strokeW = 4;
            int pad = strokeW + 8;
            int w = (int) Math.ceil(bounds.getWidth()) + pad * 2 + strokeW * 2;
            int h = fm.getAscent() + fm.getDescent() + pad * 2;
            float textX = pad - (float) bounds.getX();
            float textY = fm.getAscent() + pad;
            Shape textShape = gv.getOutline(textX, textY);

            RenderingHints rh = new RenderingHints(null);
            rh.put(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
            rh.put(RenderingHints.KEY_RENDERING,      RenderingHints.VALUE_RENDER_QUALITY);
            rh.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            // Fill (white, updated each frame)
            BufferedImage fillImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gf = fillImg.createGraphics();
            gf.setRenderingHints(rh);
            gf.setColor(Color.WHITE);
            gf.fill(textShape);
            gf.dispose();

            titleFillAlpha = new int[w * h];
            for (int y2 = 0; y2 < h; y2++)
                for (int x2 = 0; x2 < w; x2++)
                    titleFillAlpha[y2 * w + x2] = (fillImg.getRGB(x2, y2) >> 24) & 0xFF;

            titleFillNative = new NativeImage(NativeImage.Format.RGBA, w, h, false);
            titleFillDynTex = new DynamicTexture(titleFillNative);
            titleFillLoc = ResourceLocation.fromNamespaceAndPath("oyvey", "title_fill");
            Minecraft.getInstance().getTextureManager().register(titleFillLoc, titleFillDynTex);

            // Border (static)
            BufferedImage borderImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gb = borderImg.createGraphics();
            gb.setRenderingHints(rh);
            gb.setColor(Color.WHITE);
            gb.setStroke(new BasicStroke(strokeW * 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gb.draw(textShape);
            gb.dispose();

            NativeImage borderNative = new NativeImage(NativeImage.Format.RGBA, w, h, false);
            for (int y2 = 0; y2 < h; y2++) {
                for (int x2 = 0; x2 < w; x2++) {
                    int argb = borderImg.getRGB(x2, y2);
                    int r2 = (argb >> 16) & 0xFF, gr = (argb >> 8) & 0xFF,
                        b2 = argb & 0xFF, a2 = (argb >> 24) & 0xFF;
                    borderNative.setPixelRGBA(x2, y2, (a2 << 24) | (b2 << 16) | (gr << 8) | r2);
                }
            }
            titleBorderTex = ResourceLocation.fromNamespaceAndPath("oyvey", "title_border");
            Minecraft.getInstance().getTextureManager().register(titleBorderTex, new DynamicTexture(borderNative));

            titleTexW = w;
            titleTexH = h;

        } catch (Exception e) {
            titleBorderTex = null;
        }
    }

    private void renderTitle(GuiGraphics g, float delta) {
        if (titleBorderTex == null || titleFillLoc == null) return;

        waveOffset = (waveOffset + WAVE_SPEED * delta) % 360f;
        float factor = WAVE_RANGE / titleTexW;
        for (int y = 0; y < titleTexH; y++) {
            for (int x = 0; x < titleTexW; x++) {
                int a = titleFillAlpha[y * titleTexW + x];
                if (a == 0) { titleFillNative.setPixelRGBA(x, y, 0); continue; }
                float hue = (waveOffset + x * factor) % 360f;
                float[] rgb = hslToRgb(hue, 1f, 0.65f);
                int ri = (int)(rgb[0]*255), gi = (int)(rgb[1]*255), bi = (int)(rgb[2]*255);
                titleFillNative.setPixelRGBA(x, y, (a << 24) | (bi << 16) | (gi << 8) | ri);
            }
        }
        titleFillDynTex.upload();

        RenderSystem.enableBlend();
        int tx = (width - titleTexW) / 2, ty = 20;
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        g.blit(titleBorderTex, tx, ty, 0, 0, titleTexW, titleTexH, titleTexW, titleTexH);
        g.blit(titleFillLoc,   tx, ty, 0, 0, titleTexW, titleTexH, titleTexW, titleTexH);
        RenderSystem.disableBlend();
    }

    private static float[] hslToRgb(float h, float s, float l) {
        float c = (1f - Math.abs(2f * l - 1f)) * s;
        float x = c * (1f - Math.abs((h / 60f) % 2f - 1f));
        float m = l - c / 2f;
        float r, g, b;
        if      (h < 60)  { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else              { r = c; g = 0; b = x; }
        return new float[]{ r + m, g + m, b + m };
    }

    // -----------------------------------------------------------------------

    private void renderVersionInfo(GuiGraphics g) {
        String mcVersion = "Minecraft " + SharedConstants.getCurrentVersion().getName();
        String clientVersion = "HClient v" + FabricLoader.getInstance()
            .getModContainer("oyvey")
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("1.0.2");
        g.drawString(font, mcVersion,     4, height - 20, 0xFFAAAAAA, false);
        g.drawString(font, clientVersion, 4, height - 10, 0xFFAAAAAA, false);
    }

    private void rebuildRenderer() {
        PanoramaManager.Background bg = PanoramaManager.INSTANCE.getCurrent();
        if (bg.type == PanoramaManager.BackgroundType.CUBEMAP) {
            customRenderer = new PanoramaRenderer(new CubeMap(bg.cubeMapBase));
        }
    }

    private boolean handleSwitcherClick(double mouseX, double mouseY) {
        int arrowY = height - 24;
        int arrowL = width / 2 - 60;
        int arrowR = width / 2 + 44;
        if (mouseX >= arrowL && mouseX <= arrowL + 16 && mouseY >= arrowY && mouseY <= arrowY + 16) {
            PanoramaManager.INSTANCE.previous(); rebuildRenderer(); return true;
        }
        if (mouseX >= arrowR && mouseX <= arrowR + 16 && mouseY >= arrowY && mouseY <= arrowY + 16) {
            PanoramaManager.INSTANCE.next(); rebuildRenderer(); return true;
        }
        return false;
    }

    private void renderSwitcher(GuiGraphics g, int mouseX, int mouseY) {
        PanoramaManager.Background bg = PanoramaManager.INSTANCE.getCurrent();
        int    total   = PanoramaManager.INSTANCE.getCount();
        int    idx     = PanoramaManager.INSTANCE.getCurrentIndex() + 1;
        String label   = idx + "/" + total + "  " + bg.name;
        int    labelW  = font.width(label);
        int    centerX = width / 2;
        int    arrowY  = height - 24;
        int    y       = arrowY + 2;
        int    arrowL  = centerX - 60;
        int    arrowR  = centerX + 44;

        g.fill(centerX - labelW / 2 - 22, arrowY - 2,
               centerX + labelW / 2 + 22, arrowY + 18, 0x88000000);

        boolean hovL = mouseX >= arrowL && mouseX <= arrowL + 16 && mouseY >= arrowY && mouseY <= arrowY + 16;
        boolean hovR = mouseX >= arrowR && mouseX <= arrowR + 16 && mouseY >= arrowY && mouseY <= arrowY + 16;
        g.drawString(font, "◀", arrowL, y, hovL ? 0xFFFFFFFF : 0xFFAAAAAA, false);
        g.drawString(font, label, centerX - labelW / 2, y, 0xFFFFFFFF, false);
        g.drawString(font, "▶", arrowR, y, hovR ? 0xFFFFFFFF : 0xFFAAAAAA, false);
    }
}
