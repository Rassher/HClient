package me.alpha432.oyvey.features.gui.servers;

import com.mojang.blaze3d.systems.RenderSystem;
import me.alpha432.oyvey.features.gui.title.PanoramaManager;
import me.alpha432.oyvey.features.gui.title.ParticleSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class HClientEditServerScreen extends Screen {

    private final Screen parent;
    private final Consumer<ServerData> onSave;
    private final ServerData serverData;
    private final boolean isNew;

    private EditBox nameField;
    private EditBox ipField;

    private final ParticleSystem particles = new ParticleSystem();
    private PanoramaRenderer customRenderer;

    public HClientEditServerScreen(Screen parent, Consumer<ServerData> onSave, ServerData serverData, boolean isNew) {
        super(Component.literal(isNew ? "Añadir servidor" : "Editar servidor"));
        this.parent   = parent;
        this.onSave   = onSave;
        this.serverData = serverData;
        this.isNew    = isNew;
    }

    @Override
    protected void init() {
        PanoramaManager.Background bg = PanoramaManager.INSTANCE.getCurrent();
        if (bg.type == PanoramaManager.BackgroundType.CUBEMAP) {
            customRenderer = new PanoramaRenderer(new CubeMap(bg.cubeMapBase));
        }
        particles.init(width, height);

        int cx = width / 2;
        int cy = height / 2;
        int fieldW = 240;
        int fieldX = cx - fieldW / 2;

        nameField = new EditBox(font, fieldX, cy - 36, fieldW, 18, Component.literal("Nombre"));
        nameField.setMaxLength(64);
        nameField.setValue(serverData.name);
        nameField.setHint(Component.literal("Nombre del servidor").withStyle(s -> s.withColor(0x555555)));
        addRenderableWidget(nameField);

        ipField = new EditBox(font, fieldX, cy + 6, fieldW, 18, Component.literal("IP"));
        ipField.setMaxLength(128);
        ipField.setValue(serverData.ip);
        ipField.setHint(Component.literal("Dirección IP o dominio").withStyle(s -> s.withColor(0x555555)));
        addRenderableWidget(ipField);

        setInitialFocus(nameField);
    }

    @Override
    public void resize(net.minecraft.client.Minecraft mc, int w, int h) {
        String savedName = nameField != null ? nameField.getValue() : serverData.name;
        String savedIp   = ipField   != null ? ipField.getValue()   : serverData.ip;
        super.resize(mc, w, h);
        nameField.setValue(savedName);
        ipField.setValue(savedIp);
        particles.resize(w, h);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
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

        int cx = width / 2;
        int cy = height / 2;
        int cardW = 280, cardH = 120;
        drawRoundRect(g, cx - cardW / 2, cy - cardH / 2 - 10, cardW, cardH + 20, 0xCC0D0D0D, 0xFF444444);

        String titleStr = isNew ? "Añadir servidor" : "Editar servidor";
        g.drawCenteredString(font, titleStr, cx, cy - cardH / 2 + 4, 0xFFFFFFFF);

        g.drawString(font, "Nombre", cx - 120, cy - 48, 0xFFAAAAAA, false);
        g.drawString(font, "IP / Dominio", cx - 120, cy - 6, 0xFFAAAAAA, false);

        super.render(g, mouseX, mouseY, delta);

        // Custom buttons (drawn on top of EditBox widgets)
        int btnY  = cy + 36;
        int btnW  = 110;
        boolean hovSave   = isIn(mouseX, mouseY, cx - btnW - 4, btnY, btnW, 24);
        boolean hovCancel = isIn(mouseX, mouseY, cx + 4,        btnY, btnW, 24);
        drawRoundRect(g, cx - btnW - 4, btnY, btnW, 24, hovSave   ? 0xCC1A3A1A : 0xCC0D0D0D, hovSave   ? 0xFF558855 : 0xFF444444);
        drawRoundRect(g, cx + 4,        btnY, btnW, 24, hovCancel ? 0xCC222222 : 0xCC0D0D0D, hovCancel ? 0xFF888888 : 0xFF444444);
        g.drawCenteredString(font, "✔ Guardar",  cx - btnW / 2 - 4, btnY + 8, hovSave   ? 0xFF88FF88 : 0xFFFFFFFF);
        g.drawCenteredString(font, "✕ Cancelar", cx + btnW / 2 + 4, btnY + 8, hovCancel ? 0xFFFFFFFF : 0xFFAAAAAA);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int cx   = width / 2;
            int cy   = height / 2;
            int btnY = cy + 36;
            int btnW = 110;
            if (isIn(mx, my, cx - btnW - 4, btnY, btnW, 24)) { save();   return true; }
            if (isIn(mx, my, cx + 4,        btnY, btnW, 24)) { cancel(); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // ENTER / NUMPAD_ENTER
            save();
            return true;
        }
        if (keyCode == 256) { // ESCAPE
            cancel();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void save() {
        String name = nameField.getValue().trim();
        String ip   = ipField.getValue().trim();
        if (name.isEmpty() || ip.isEmpty()) return;
        serverData.name = name;
        serverData.ip   = ip;
        onSave.accept(serverData);
        minecraft.setScreen(parent);
    }

    private void cancel() {
        minecraft.setScreen(parent);
    }

    private static boolean isIn(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
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
}
