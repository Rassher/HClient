package me.alpha432.oyvey.features.gui.title;

import com.mojang.blaze3d.platform.NativeImage;
import me.alpha432.oyvey.features.gui.HClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manages background options for the custom title screen.
 * CubeMap in 1.21.1 takes a single base ResourceLocation and internally
 * appends _0.._5, so we register textures matching that pattern.
 */
public class PanoramaManager {

    public static final PanoramaManager INSTANCE = new PanoramaManager();

    public enum BackgroundType { CUBEMAP, PARTICLES }

    public static class Background {
        public final BackgroundType type;
        public final String name;
        /** Base location passed to new CubeMap(base). Null when type == PARTICLES. */
        public final ResourceLocation cubeMapBase;

        Background(BackgroundType type, String name, ResourceLocation cubeMapBase) {
            this.type        = type;
            this.name        = name;
            this.cubeMapBase = cubeMapBase;
        }
    }

    // Vanilla base — CubeMap will resolve panorama_0..5 from here
    private static final ResourceLocation VANILLA_BASE =
        ResourceLocation.withDefaultNamespace("textures/gui/title/background/panorama");

    private final List<Background> options = new ArrayList<>();
    private int currentIndex = 0;

    private PanoramaManager() {}

    public void init() {
        options.clear();

        options.add(new Background(BackgroundType.PARTICLES, "RGB Particles", null));

        File baseDir = getPanoramasDir();
        if (!baseDir.exists()) baseDir.mkdirs();

        File[] sets = baseDir.listFiles(File::isDirectory);
        if (sets != null) {
            Arrays.sort(sets);
            for (File set : sets) {
                ResourceLocation base = tryLoadCubemap(set);
                if (base != null)
                    options.add(new Background(BackgroundType.CUBEMAP, set.getName(), base));
            }
        }

        options.add(new Background(BackgroundType.CUBEMAP, "vanilla", VANILLA_BASE));

        // Restore saved background
        HClientConfig.INSTANCE.load();
        String saved = HClientConfig.INSTANCE.getBackground();
        currentIndex = 0;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).name.equals(saved)) { currentIndex = i; break; }
        }
    }

    public Background next() {
        currentIndex = (currentIndex + 1) % options.size();
        HClientConfig.INSTANCE.setBackground(getCurrent().name);
        return getCurrent();
    }

    public Background previous() {
        currentIndex = (currentIndex - 1 + options.size()) % options.size();
        HClientConfig.INSTANCE.setBackground(getCurrent().name);
        return getCurrent();
    }

    public Background getCurrent()      { return options.get(currentIndex); }
    public int        getCurrentIndex() { return currentIndex; }
    public int        getCount()        { return options.size(); }

    // -----------------------------------------------------------------------

    /**
     * Registers the 6 face textures and returns the base ResourceLocation
     * that CubeMap will use (it appends _0.._5 internally).
     */
    private ResourceLocation tryLoadCubemap(File dir) {
        // Base path: oyvey:textures/panoramas/<name>/panorama
        // CubeMap will look for:  oyvey:textures/panoramas/<name>/panorama_0 .. _5
        String name = dir.getName();
        for (int i = 0; i < 6; i++) {
            File png = new File(dir, "panorama_" + i + ".png");
            if (!png.exists()) return null;
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
                "oyvey", "textures/panoramas/" + name + "/panorama_" + i);
            if (!registerTexture(loc, png)) return null;
        }
        return ResourceLocation.fromNamespaceAndPath(
            "oyvey", "textures/panoramas/" + name + "/panorama");
    }

    private boolean registerTexture(ResourceLocation loc, File file) {
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) return false;
            int w = img.getWidth(), h = img.getHeight();
            int[] pixels = img.getRGB(0, 0, w, h, null, 0, w);

            NativeImage native_ = new NativeImage(NativeImage.Format.RGBA, w, h, false);
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    int argb = pixels[y * w + x];
                    int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF,
                        b = argb & 0xFF,          a = (argb >> 24) & 0xFF;
                    native_.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
                }

            Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(native_));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private File getPanoramasDir() {
        return new File(Minecraft.getInstance().gameDirectory, "hclient/panoramas");
    }
}
