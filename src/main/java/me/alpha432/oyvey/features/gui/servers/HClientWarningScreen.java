package me.alpha432.oyvey.features.gui.servers;

import com.mojang.blaze3d.systems.RenderSystem;
import me.alpha432.oyvey.features.gui.HClientConfig;
import me.alpha432.oyvey.features.gui.title.PanoramaManager;
import me.alpha432.oyvey.features.gui.title.ParticleSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;

public class HClientWarningScreen extends Screen {

    private static final int COOLDOWN_TICKS = 100; // 5 segundos

    private final Screen parent;
    private final ServerData serverData;
    private final boolean isDanger;   // true = Peligro (3), false = Sin Analizar (-1)

    private int ticksElapsed = 0;
    private boolean suppressChecked = false;

    private final ParticleSystem particles = new ParticleSystem();
    private PanoramaRenderer customRenderer;

    public HClientWarningScreen(Screen parent, ServerData serverData, boolean isDanger) {
        super(Component.literal("Advertencia"));
        this.parent     = parent;
        this.serverData = serverData;
        this.isDanger   = isDanger;
    }

    @Override
    protected void init() {
        suppressChecked = HClientConfig.INSTANCE.isSuppressRiskWarning();
        PanoramaManager.Background bg = PanoramaManager.INSTANCE.getCurrent();
        if (bg.type == PanoramaManager.BackgroundType.CUBEMAP) {
            customRenderer = new PanoramaRenderer(new CubeMap(bg.cubeMapBase));
        }
        particles.init(width, height);
    }

    @Override
    public void resize(net.minecraft.client.Minecraft mc, int w, int h) {
        super.resize(mc, w, h);
        particles.resize(w, h);
    }

    @Override
    public void tick() {
        super.tick();
        if (ticksElapsed < COOLDOWN_TICKS) ticksElapsed++;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // Background
        PanoramaManager.Background bg = PanoramaManager.INSTANCE.getCurrent();
        if (bg.type == PanoramaManager.BackgroundType.PARTICLES) {
            g.fill(0, 0, width, height, 0xFF141414);
            particles.tick(delta);
            particles.render(g);
        } else {
            RenderSystem.enableBlend();
            customRenderer.render(g, width, height, 1.0f, delta);
            RenderSystem.disableBlend();
            g.fill(0, 0, width, height, 0x73000000);
        }

        int cx = width / 2, cy = height / 2;
        int cardW = 360, cardH = 220;
        int cardX = cx - cardW / 2, cardY = cy - cardH / 2;

        // Dark overlay behind card
        g.fill(0, 0, width, height, 0x66000000);
        drawRoundRect(g, cardX, cardY, cardW, cardH, 0xEE0D0D0D, isDanger ? 0xFFAA3333 : 0xFF5555AA);

        // Icon + title
        String icon  = isDanger ? "⚠" : "?";
        int iconColor = isDanger ? 0xFFFF5555 : 0xFF8888FF;
        g.drawCenteredString(font, icon + "  " + (isDanger ? "Servidor Peligroso" : "Servidor Sin Analizar"), cx, cardY + 16, iconColor);
        drawHLine(g, cardX + 16, cardY + 30, cardW - 32, 0x44FFFFFF);

        // Body text
        int textY = cardY + 40;
        int textColor = 0xFFCCCCCC;
        int textX = cardX + 20;
        int textW = cardW - 40;

        if (isDanger) {
            drawWrapped(g, "Este servidor tiene un sistema anti-cheat robusto que detecta", textX, textY,      textW, textColor);
            drawWrapped(g, "modificaciones como las de HClient.",                             textX, textY+12, textW, textColor);
            drawWrapped(g, "Conectarte puede resultar en:",                                   textX, textY+28, textW, 0xFFFFAAAA);
            drawWrapped(g, "• Baneo permanente de tu cuenta",                                 textX, textY+40, textW, 0xFFFF8888);
            drawWrapped(g, "• Baneo de IP o HWID",                                           textX, textY+52, textW, 0xFFFF8888);
            drawWrapped(g, "• Pérdida de tu progreso en el servidor",                        textX, textY+64, textW, 0xFFFF8888);
        } else {
            drawWrapped(g, "Este servidor no está en nuestra base de datos.",                 textX, textY,      textW, textColor);
            drawWrapped(g, "No tenemos información sobre su sistema anti-cheat.",             textX, textY+12, textW, textColor);
            drawWrapped(g, "Conectarte podría resultar en:",                                  textX, textY+28, textW, 0xFFFFEEAA);
            drawWrapped(g, "• Baneo si el servidor detecta modificaciones",                  textX, textY+40, textW, 0xFFFFCC88);
            drawWrapped(g, "• Comportamiento impredecible del anti-cheat",                   textX, textY+52, textW, 0xFFFFCC88);
        }

        // Checkbox "No volver a mostrar"
        int checkY = cardY + cardH - 46;
        drawCheckbox(g, cardX + 20, checkY, suppressChecked, mouseX, mouseY);
        g.drawString(font, "No volver a mostrar esta advertencia", cardX + 36, checkY + 2, 0xFFAAAAAA, false);

        // Buttons
        int btnY   = cardY + cardH - 28;
        int btnW   = 130;
        boolean hovCancel = isIn(mouseX, mouseY, cardX + 16,              btnY, btnW, 22);
        boolean hovEnter  = isIn(mouseX, mouseY, cardX + cardW - btnW - 16, btnY, btnW, 22) && ticksElapsed >= COOLDOWN_TICKS;

        drawRoundRect(g, cardX + 16, btnY, btnW, 22, hovCancel ? 0xCC2A0A0A : 0xCC1A0A0A, 0xFF884444);
        g.drawCenteredString(font, "✕ Cancelar", cardX + 16 + btnW / 2, btnY + 7, hovCancel ? 0xFFFF8888 : 0xFFCC6666);

        int enterX = cardX + cardW - btnW - 16;
        if (ticksElapsed < COOLDOWN_TICKS) {
            int secsLeft = (int) Math.ceil((COOLDOWN_TICKS - ticksElapsed) / 20.0);
            drawRoundRect(g, enterX, btnY, btnW, 22, 0xCC111111, 0xFF444444);
            g.drawCenteredString(font, "Entrar (" + secsLeft + "s)", enterX + btnW / 2, btnY + 7, 0xFF666666);
        } else {
            drawRoundRect(g, enterX, btnY, btnW, 22, hovEnter ? 0xCC0A2A0A : 0xCC0A1A0A, hovEnter ? 0xFF55AA55 : 0xFF446644);
            g.drawCenteredString(font, "▶ Entrar", enterX + btnW / 2, btnY + 7, hovEnter ? 0xFF88FF88 : 0xFF66AA66);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        int cx = width / 2, cy = height / 2;
        int cardW = 360, cardH = 220;
        int cardX = cx - cardW / 2, cardY = cy - cardH / 2;

        // Checkbox
        int checkY = cardY + cardH - 46;
        if (isIn(mx, my, cardX + 20, checkY, 13, 13)) {
            suppressChecked = !suppressChecked;
            return true;
        }

        // Cancel
        if (isIn(mx, my, cardX + 16, cardY + cardH - 28, 130, 22)) {
            minecraft.setScreen(parent);
            return true;
        }

        // Enter (only after cooldown)
        if (ticksElapsed >= COOLDOWN_TICKS && isIn(mx, my, cardX + cardW - 146, cardY + cardH - 28, 130, 22)) {
            if (suppressChecked) HClientConfig.INSTANCE.setSuppressRiskWarning(true);
            ConnectScreen.startConnecting(parent, minecraft, ServerAddress.parseString(serverData.ip), serverData, false, null);
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { minecraft.setScreen(parent); return true; } // ESC
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // -----------------------------------------------------------------------

    private void drawCheckbox(GuiGraphics g, int x, int y, boolean checked, int mx, int my) {
        boolean hov = isIn(mx, my, x, y, 13, 13);
        g.fill(x, y, x + 13, y + 13, hov ? 0xFF222222 : 0xFF111111);
        g.fill(x, y, x + 13, y + 1, 0xFF555555);
        g.fill(x, y + 12, x + 13, y + 13, 0xFF555555);
        g.fill(x, y, x + 1, y + 13, 0xFF555555);
        g.fill(x + 12, y, x + 13, y + 13, 0xFF555555);
        if (checked) {
            g.drawCenteredString(font, "✔", x + 7, y + 2, 0xFF55FF55);
        }
    }

    private void drawWrapped(GuiGraphics g, String text, int x, int y, int maxW, int color) {
        g.drawString(font, font.substrByWidth(Component.literal(text), maxW).getString(), x, y, color, false);
    }

    private void drawHLine(GuiGraphics g, int x, int y, int w, int color) {
        g.fill(x, y, x + w, y + 1, color);
    }

    private void drawRoundRect(GuiGraphics g, int x, int y, int w, int h, int bg, int border) {
        g.fill(x + 1, y, x + w - 1, y + h, bg);
        g.fill(x, y + 1, x + 1, y + h - 1, bg);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, bg);
        g.fill(x + 1, y, x + w - 1, y + 1, border);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, border);
        g.fill(x, y + 1, x + 1, y + h - 1, border);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, border);
    }

    private static boolean isIn(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
