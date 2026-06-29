package me.alpha432.oyvey.features.gui.servers;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import me.alpha432.oyvey.features.gui.title.PanoramaManager;
import me.alpha432.oyvey.features.gui.title.ParticleSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

public class HClientServerListScreen extends Screen {

    private final Screen lastScreen;
    private final ParticleSystem particles = new ParticleSystem();
    private PanoramaRenderer customRenderer;

    private ServerList serverList;
    private final ServerStatusPinger pinger = new ServerStatusPinger();
    private final Map<String, ResourceLocation> iconCache = new HashMap<>();

    private int selected = -1;
    private float scroll = 0f;

    private static final int CARD_H   = 50;
    private static final int CARD_GAP = 6;
    private static final int MARGIN_X = 40;
    private static final int LIST_TOP = 46;
    private static final int BAR_H    = 36;

    public HClientServerListScreen(Screen lastScreen) {
        super(Component.literal("HClient Servers"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        AntiCheatDB.load();
        PanoramaManager.Background bg = PanoramaManager.INSTANCE.getCurrent();
        if (bg.type == PanoramaManager.BackgroundType.CUBEMAP) {
            customRenderer = new PanoramaRenderer(new CubeMap(bg.cubeMapBase));
        }
        particles.init(width, height);
        loadServers();
    }

    @Override
    public void resize(Minecraft mc, int w, int h) {
        super.resize(mc, w, h);
        particles.resize(w, h);
    }

    private void loadServers() {
        serverList = new ServerList(minecraft);
        serverList.load();
        for (int i = 0; i < serverList.size(); i++) {
            ServerData sd = serverList.get(i);
            try { pinger.pingServer(sd, () -> {}, () -> {}); } catch (Exception ignored) {}
        }
        if (selected >= serverList.size()) selected = serverList.size() - 1;
    }

    @Override
    public void tick() {
        super.tick();
        pinger.tick();
    }

    @Override
    public void onClose() {
        pinger.removeAll();
        minecraft.setScreen(lastScreen);
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

        g.drawCenteredString(font, "Servidores", width / 2, 14, 0xFFFFFFFF);

        renderServerList(g, mouseX, mouseY);
        renderBottomBar(g, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    // -----------------------------------------------------------------------
    // Server cards

    private int listBottom() { return height - BAR_H - 8; }
    private int cardWidth()  { return width - MARGIN_X * 2; }

    private void renderServerList(GuiGraphics g, int mouseX, int mouseY) {
        int top = LIST_TOP;
        int bottom = listBottom();
        g.enableScissor(MARGIN_X, top, width - MARGIN_X, bottom);

        int y = (int) (top - scroll);
        for (int i = 0; i < serverList.size(); i++) {
            int cardY = y + i * (CARD_H + CARD_GAP);
            if (cardY + CARD_H >= top && cardY <= bottom) {
                renderCard(g, serverList.get(i), i, MARGIN_X, cardY, cardWidth(), mouseX, mouseY);
            }
        }
        g.disableScissor();
    }

    private void renderCard(GuiGraphics g, ServerData data, int index, int x, int y, int w, int mouseX, int mouseY) {
        boolean hov = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + CARD_H
                      && mouseY >= LIST_TOP && mouseY <= listBottom();
        boolean sel = index == selected;

        int bg = sel ? 0xCC2A2A2A : (hov ? 0xCC1C1C1C : 0xCC0D0D0D);
        int border = sel ? 0xFFAAAAAA : 0xFF444444;
        drawRoundRect(g, x, y, w, CARD_H, bg, border);

        // Icon
        int iconSz = 32;
        ResourceLocation icon = getOrLoadIcon(data);
        int iconX = x + 8, iconY = y + (CARD_H - iconSz) / 2;
        if (icon != null) {
            g.blit(icon, iconX, iconY, 0, 0, iconSz, iconSz, iconSz, iconSz);
        } else {
            g.fill(iconX, iconY, iconX + iconSz, iconY + iconSz, 0xFF333333);
        }

        int textX = iconX + iconSz + 10;

        // Anti-cheat badge (rightmost)
        int lvl = AntiCheatDB.getLevel(data.ip);
        String label = AntiCheatDB.getLevelLabel(lvl);
        int acColor = AntiCheatDB.getLevelColor(lvl);
        int labelW = font.width(label);
        int badgeX = x + w - labelW - 14;
        g.fill(badgeX - 6, y + CARD_H / 2 - 7, badgeX + labelW + 6, y + CARD_H / 2 + 7, 0x22000000);
        g.drawString(font, label, badgeX, y + CARD_H / 2 - 4, acColor, false);

        // Ping (left of AC badge)
        String pingStr = data.ping > 0 ? String.valueOf(data.ping) : "...";
        int pingColor = data.ping <= 0 ? 0xFFAAAAAA
                      : data.ping <= 100 ? 0xFF55FF55
                      : data.ping <= 200 ? 0xFFFFAA00
                      : 0xFFFF5555;
        int pingW = font.width(pingStr);
        int pingX = badgeX - pingW - 14;
        g.drawString(font, pingStr, pingX, y + CARD_H / 2 - 4, pingColor, false);

        // Name + info (left)
        g.drawString(font, data.name, textX, y + 9, 0xFFFFFFFF, false);
        String players = data.players != null ? data.players.online() + "/" + data.players.max() : "-/-";
        g.drawString(font, data.ip + "  •  " + players, textX, y + 26, 0xFFAAAAAA, false);
    }

    private ResourceLocation getOrLoadIcon(ServerData data) {
        byte[] icon = data.getIconBytes();
        if (icon == null) return null;
        ResourceLocation cached = iconCache.get(data.ip);
        if (cached != null) return cached;
        try {
            NativeImage img = NativeImage.read(new ByteArrayInputStream(icon));
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
                "oyvey", "servericon/" + Integer.toHexString(data.ip.hashCode()));
            minecraft.getTextureManager().register(loc, new DynamicTexture(img));
            iconCache.put(data.ip, loc);
            return loc;
        } catch (Exception e) {
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Bottom bar

    private void renderBottomBar(GuiGraphics g, int mouseX, int mouseY) {
        int y = height - BAR_H - 4;
        drawBarBtn(g, 8, y, 70, 26, "← Atrás", mouseX, mouseY, true);

        int bw = 90, gap = 6;
        int totalW = bw * 4 + gap * 3;
        int startX = width - 8 - totalW;
        drawBarBtn(g, startX, y, bw, 26, "+ Añadir", mouseX, mouseY, true);
        drawBarBtn(g, startX + (bw + gap), y, bw, 26, "✎ Editar", mouseX, mouseY, selected >= 0);
        drawBarBtn(g, startX + (bw + gap) * 2, y, bw, 26, "✕ Borrar", mouseX, mouseY, selected >= 0);
        drawBarBtn(g, startX + (bw + gap) * 3, y, bw, 26, "▶ Conectar", mouseX, mouseY, selected >= 0);
    }

    private void drawBarBtn(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my, boolean enabled) {
        boolean hov = enabled && mx >= x && mx <= x + w && my >= y && my <= y + h;
        int bg = !enabled ? 0x661A1A1A : (hov ? 0xCC222222 : 0xCC0D0D0D);
        int border = !enabled ? 0xFF333333 : (hov ? 0xFF888888 : 0xFF444444);
        drawRoundRect(g, x, y, w, h, bg, border);
        int textColor = enabled ? 0xFFFFFFFF : 0xFF666666;
        g.drawCenteredString(font, label, x + w / 2, y + h / 2 - font.lineHeight / 2, textColor);
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

    // -----------------------------------------------------------------------
    // Input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // Server cards
        if (mouseY >= LIST_TOP && mouseY <= listBottom()) {
            int top = LIST_TOP;
            int y = (int) (top - scroll);
            for (int i = 0; i < serverList.size(); i++) {
                int cardY = y + i * (CARD_H + CARD_GAP);
                if (mouseX >= MARGIN_X && mouseX <= MARGIN_X + cardWidth()
                    && mouseY >= cardY && mouseY <= cardY + CARD_H) {
                    if (selected == i) { connect(); } else { selected = i; }
                    return true;
                }
            }
        }

        int by = height - BAR_H - 4;
        if (isIn(mouseX, mouseY, 8, by, 70, 26)) { onClose(); return true; }

        int bw = 90, gap = 6;
        int totalW = bw * 4 + gap * 3;
        int startX = width - 8 - totalW;
        if (isIn(mouseX, mouseY, startX, by, bw, 26)) { addServer(); return true; }
        if (selected >= 0) {
            if (isIn(mouseX, mouseY, startX + (bw + gap), by, bw, 26)) { editServer(); return true; }
            if (isIn(mouseX, mouseY, startX + (bw + gap) * 2, by, bw, 26)) { deleteServer(); return true; }
            if (isIn(mouseX, mouseY, startX + (bw + gap) * 3, by, bw, 26)) { connect(); return true; }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= scrollY * 20;
        float maxScroll = Math.max(0, serverList.size() * (CARD_H + CARD_GAP) - (listBottom() - LIST_TOP));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        return true;
    }

    private static boolean isIn(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    // -----------------------------------------------------------------------
    // Actions

    private void connect() {
        if (selected < 0 || selected >= serverList.size()) return;
        ServerData data = serverList.get(selected);
        int lvl = AntiCheatDB.getLevel(data.ip);

        if (AntiCheatDB.isRisky(lvl) && !me.alpha432.oyvey.features.gui.HClientConfig.INSTANCE.isSuppressRiskWarning()) {
            minecraft.setScreen(new HClientWarningScreen(this, data, lvl == 3));
            return;
        }
        ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(data.ip), data, false, null);
    }

    private void addServer() {
        ServerData fresh = new ServerData("Nuevo servidor", "", ServerData.Type.OTHER);
        minecraft.setScreen(new HClientEditServerScreen(this, added -> {
            serverList.add(added, false);
            serverList.save();
            loadServers();
        }, fresh, true));
    }

    private void editServer() {
        if (selected < 0 || selected >= serverList.size()) return;
        ServerData data = serverList.get(selected);
        minecraft.setScreen(new HClientEditServerScreen(this, edited -> {
            serverList.replace(selected, edited);
            serverList.save();
            loadServers();
        }, data, false));
    }

    private void deleteServer() {
        if (selected < 0 || selected >= serverList.size()) return;
        serverList.remove(serverList.get(selected));
        serverList.save();
        selected = -1;
        loadServers();
    }
}
