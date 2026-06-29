package me.alpha432.oyvey.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public final class PokemonSpriteCache {

    private static final String URL_NORMAL = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/%d.png";
    private static final String URL_SHINY  = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/shiny/%d.png";

    private static final Map<String, ResourceLocation> CACHE   = new ConcurrentHashMap<>();
    private static final Set<String>                   PENDING = ConcurrentHashMap.newKeySet();
    private static final ExecutorService               POOL    = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "pokemon-sprite-dl");
        t.setDaemon(true);
        return t;
    });

    private PokemonSpriteCache() {}

    /**
     * Returns the cached ResourceLocation for this sprite, or null if it is not
     * yet loaded. The first call for a given dex/shiny combo kicks off an async
     * download; subsequent calls return null until the download completes, then
     * return the registered texture.
     */
    public static ResourceLocation get(int dexNumber, boolean shiny) {
        if (dexNumber <= 0) return null;
        String key = dexNumber + (shiny ? "_s" : "");
        ResourceLocation cached = CACHE.get(key);
        if (cached != null) return cached;
        if (!PENDING.add(key)) return null; // already downloading

        String urlStr = String.format(shiny ? URL_SHINY : URL_NORMAL, dexNumber);
        POOL.submit(() -> {
            NativeImage img = download(urlStr);
            if (img == null) {
                // try normal sprite as fallback for shiny (not all have shiny sprites)
                if (shiny) img = download(String.format(URL_NORMAL, dexNumber));
            }
            if (img == null) { PENDING.remove(key); return; }
            final NativeImage finalImg = img;
            Minecraft.getInstance().execute(() -> {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                        "hclient", "pokemon/sprite/" + key);
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(finalImg));
                CACHE.put(key, id);
                PENDING.remove(key);
            });
        });
        return null;
    }

    private static NativeImage download(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "HClient/" + BuildConfig.VERSION);
            conn.connect();
            if (conn.getResponseCode() != 200) return null;
            try (InputStream in = conn.getInputStream()) {
                return NativeImage.read(in);
            }
        } catch (Exception e) { return null; }
    }

    /** Call on world unload to free GPU textures. */
    public static void clear() {
        Minecraft mc = Minecraft.getInstance();
        CACHE.forEach((k, rl) -> mc.getTextureManager().release(rl));
        CACHE.clear();
        PENDING.clear();
    }
}
