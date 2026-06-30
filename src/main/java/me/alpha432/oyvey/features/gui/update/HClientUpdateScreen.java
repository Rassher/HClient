package me.alpha432.oyvey.features.gui.update;

import com.mojang.blaze3d.systems.RenderSystem;
import me.alpha432.oyvey.features.gui.title.PanoramaManager;
import me.alpha432.oyvey.features.gui.title.ParticleSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;

public class HClientUpdateScreen extends Screen {

    private static final Logger LOGGER = LoggerFactory.getLogger("HClient-Updater");

    private final Screen parent;
    private final String latestVersion;
    private final String downloadUrl;

    private final ParticleSystem particles = new ParticleSystem();
    private PanoramaRenderer customRenderer;

    // Download state
    public enum DlState { IDLE, DOWNLOADING, DONE, ERROR }
    private volatile DlState dlState   = DlState.IDLE;
    private volatile float   progress  = 0f; // 0..1
    private volatile String  errorMsg  = "";

    public HClientUpdateScreen(Screen parent, String latestVersion, String downloadUrl) {
        super(Component.literal("Actualización disponible"));
        this.parent        = parent;
        this.latestVersion = latestVersion;
        this.downloadUrl   = downloadUrl;
    }

    @Override
    protected void init() {
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
        int cardW = 380, cardH = 200;
        int cardX = cx - cardW / 2, cardY = cy - cardH / 2;

        g.fill(0, 0, width, height, 0x55000000);
        drawRoundRect(g, cardX, cardY, cardW, cardH, 0xEE0D0D0D, 0xFF4488AA);

        // Header
        g.drawCenteredString(font, "⬆  Nueva versión disponible", cx, cardY + 14, 0xFF88CCFF);
        drawHLine(g, cardX + 16, cardY + 28, cardW - 32, 0x44FFFFFF);

        // Version info
        String current = FabricLoader.getInstance()
            .getModContainer("oyvey")
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("?");
        g.drawCenteredString(font, "Versión actual: " + current + "  →  " + latestVersion, cx, cardY + 42, 0xFFCCCCCC);

        // Reason
        g.drawCenteredString(font, "Actualiza para obtener las últimas mejoras de seguridad", cx, cardY + 60, 0xFFAAAAAA);
        g.drawCenteredString(font, "y compatibilidad con la base de datos anti-cheat.",        cx, cardY + 72, 0xFFAAAAAA);

        renderButtons(g, mouseX, mouseY, cardX, cardY, cardW, cardH);
    }

    private void renderButtons(GuiGraphics g, int mx, int my, int cardX, int cardY, int cardW, int cardH) {
        int cx = cardX + cardW / 2;
        int btnY = cardY + cardH - 46;

        if (dlState == DlState.IDLE) {
            // Skip
            boolean hovSkip = isIn(mx, my, cardX + 16, btnY, 130, 24);
            drawRoundRect(g, cardX + 16, btnY, 130, 24, hovSkip ? 0xCC222222 : 0xCC111111, 0xFF444444);
            g.drawCenteredString(font, "Ahora no", cardX + 16 + 65, btnY + 8, hovSkip ? 0xFFFFFFFF : 0xFF888888);

            // Download
            boolean hovDl = isIn(mx, my, cardX + cardW - 146, btnY, 130, 24);
            drawRoundRect(g, cardX + cardW - 146, btnY, 130, 24,
                hovDl ? 0xCC0A2A3A : 0xCC0A1A2A, hovDl ? 0xFF44AACC : 0xFF226688);
            g.drawCenteredString(font, "⬇ Descargar e instalar", cardX + cardW - 81, btnY + 8,
                hovDl ? 0xFF88DDFF : 0xFF44AACC);

        } else if (dlState == DlState.DOWNLOADING) {
            // Progress bar
            int barW = cardW - 80;
            int barX = cardX + 40, barY2 = btnY + 4;
            g.fill(barX, barY2, barX + barW, barY2 + 16, 0xFF1A1A1A);
            g.fill(barX, barY2, barX + (int)(barW * progress), barY2 + 16, 0xFF2266AA);
            g.fill(barX, barY2, barX + barW, barY2 + 1, 0xFF444444);
            g.fill(barX, barY2 + 15, barX + barW, barY2 + 16, 0xFF444444);
            g.drawCenteredString(font, (int)(progress * 100) + "%", cx, barY2 + 4, 0xFFFFFFFF);
            g.drawCenteredString(font, "Descargando...", cx, btnY - 14, 0xFFAAAAAA);

        } else if (dlState == DlState.DONE) {
            drawRoundRect(g, cardX + 40, btnY, cardW - 80, 24, 0xCC0A2A0A, 0xFF44AA44);
            g.drawCenteredString(font, "✔ Instalado — reinicia Minecraft para aplicar", cx, btnY + 8, 0xFF88FF88);
            // Close button
            boolean hovClose = isIn(mx, my, cx - 50, btnY + 30, 100, 22);
            drawRoundRect(g, cx - 50, btnY + 30, 100, 22, hovClose ? 0xCC222222 : 0xCC111111, 0xFF555555);
            g.drawCenteredString(font, "Cerrar", cx, btnY + 38, 0xFFFFFFFF);

        } else if (dlState == DlState.ERROR) {
            g.drawCenteredString(font, "✕ Error: " + errorMsg, cx, btnY + 4, 0xFFFF6666);
            boolean hovRetry = isIn(mx, my, cx - 50, btnY + 18, 100, 22);
            drawRoundRect(g, cx - 50, btnY + 18, 100, 22, hovRetry ? 0xCC222222 : 0xCC111111, 0xFF555555);
            g.drawCenteredString(font, "Reintentar", cx, btnY + 26, 0xFFFFFFFF);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        int cx = width / 2, cy = height / 2;
        int cardW = 380, cardH = 200;
        int cardX = cx - cardW / 2, cardY = cy - cardH / 2;
        int btnY = cardY + cardH - 46;

        if (dlState == DlState.IDLE) {
            if (isIn(mx, my, cardX + 16, btnY, 130, 24)) {
                minecraft.setScreen(parent);
                return true;
            }
            if (isIn(mx, my, cardX + cardW - 146, btnY, 130, 24)) {
                startDownload();
                return true;
            }
        } else if (dlState == DlState.DONE) {
            if (isIn(mx, my, cx - 50, btnY + 30, 100, 22)) {
                minecraft.setScreen(parent);
                return true;
            }
        } else if (dlState == DlState.ERROR) {
            if (isIn(mx, my, cx - 50, btnY + 18, 100, 22)) {
                dlState = DlState.IDLE;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256 && dlState == DlState.IDLE) { minecraft.setScreen(parent); return true; }
        return super.keyPressed(key, scan, mods);
    }

    // -----------------------------------------------------------------------

    private void startDownload() {
        dlState  = DlState.DOWNLOADING;
        progress = 0f;

        Thread t = new Thread(() -> {
            try {
                Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
                File dest = modsDir.resolve("HClient-" + latestVersion + ".jar").toFile();

                // Delete old HClient JARs (oyvey-*.jar and HClient-*.jar)
                File[] old = modsDir.toFile().listFiles(f ->
                    (f.getName().startsWith("oyvey-") || f.getName().startsWith("HClient-"))
                    && f.getName().endsWith(".jar") && !f.equals(dest));
                if (old != null) Arrays.stream(old).forEach(f -> {
                    if (f.delete()) LOGGER.info("Deleted old JAR: {}", f.getName());
                });

                // Download new JAR with progress tracking
                HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .header("User-Agent", "HClient-Updater")
                    .timeout(Duration.ofMinutes(2))
                    .GET().build();

                // Get content length first
                HttpRequest headReq = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .header("User-Agent", "HClient-Updater")
                    .method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
                long total = -1;
                try {
                    var headResp = client.send(headReq, HttpResponse.BodyHandlers.discarding());
                    total = headResp.headers().firstValueAsLong("content-length").orElse(-1);
                } catch (Exception ignored) {}

                final long totalBytes = total;
                HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());

                try (InputStream in = resp.body(); FileOutputStream out = new FileOutputStream(dest)) {
                    byte[] buf = new byte[8192];
                    long downloaded = 0;
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                        downloaded += n;
                        if (totalBytes > 0) progress = (float) downloaded / totalBytes;
                    }
                }

                progress = 1f;
                dlState  = DlState.DONE;
                LOGGER.info("Downloaded HClient {} to {}", latestVersion, dest.getName());

            } catch (Exception e) {
                LOGGER.error("Download failed", e);
                errorMsg = e.getMessage() != null ? e.getMessage().substring(0, Math.min(40, e.getMessage().length())) : "Unknown";
                dlState  = DlState.ERROR;
            }
        }, "HClient-Downloader");
        t.setDaemon(true);
        t.start();
    }

    // -----------------------------------------------------------------------

    private void drawRoundRect(GuiGraphics g, int x, int y, int w, int h, int bg, int border) {
        g.fill(x + 1, y, x + w - 1, y + h, bg);
        g.fill(x, y + 1, x + 1, y + h - 1, bg);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, bg);
        g.fill(x + 1, y, x + w - 1, y + 1, border);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, border);
        g.fill(x, y + 1, x + 1, y + h - 1, border);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, border);
    }

    private void drawHLine(GuiGraphics g, int x, int y, int w, int color) {
        g.fill(x, y, x + w, y + 1, color);
    }

    private static boolean isIn(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
