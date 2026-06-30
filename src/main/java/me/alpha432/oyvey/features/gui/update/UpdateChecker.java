package me.alpha432.oyvey.features.gui.update;

import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class UpdateChecker {

    public static final UpdateChecker INSTANCE = new UpdateChecker();
    private static final Logger LOGGER = LoggerFactory.getLogger("HClient-Updater");
    private static final String API_URL = "https://api.github.com/repos/Rassher/HClient/releases/latest";

    public enum State { PENDING, UP_TO_DATE, UPDATE_AVAILABLE, ERROR }

    private volatile State   state        = State.PENDING;
    private volatile String  latestVersion;
    private volatile String  downloadUrl;

    private UpdateChecker() {}

    public void checkAsync() {
        Thread t = new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(8))
                    .build();
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "HClient-Updater")
                    .timeout(Duration.ofSeconds(10))
                    .GET().build();

                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 200) { state = State.ERROR; return; }

                var json = JsonParser.parseString(resp.body()).getAsJsonObject();
                String tag = json.get("tag_name").getAsString(); // e.g. "v1.0.4"
                latestVersion = tag.startsWith("v") ? tag.substring(1) : tag;

                // Find JAR asset (exclude sources)
                var assets = json.getAsJsonArray("assets");
                for (var el : assets) {
                    var asset = el.getAsJsonObject();
                    String name = asset.get("name").getAsString();
                    if (name.endsWith(".jar") && !name.contains("sources")) {
                        downloadUrl = asset.get("browser_download_url").getAsString();
                        break;
                    }
                }

                String current = FabricLoader.getInstance()
                    .getModContainer("oyvey")
                    .map(c -> c.getMetadata().getVersion().getFriendlyString())
                    .orElse("0.0.0");

                state = isNewer(latestVersion, current) ? State.UPDATE_AVAILABLE : State.UP_TO_DATE;
                LOGGER.info("Version check: current={} latest={} state={}", current, latestVersion, state);

            } catch (Exception e) {
                LOGGER.warn("Update check failed: {}", e.getMessage());
                state = State.ERROR;
            }
        }, "HClient-UpdateChecker");
        t.setDaemon(true);
        t.start();
    }

    public State  getState()         { return state; }
    public String getLatestVersion() { return latestVersion; }
    public String getDownloadUrl()   { return downloadUrl; }

    /** Returns true if candidate is strictly newer than current (semver). */
    private static boolean isNewer(String candidate, String current) {
        try {
            int[] c = parse(current), n = parse(candidate);
            for (int i = 0; i < 3; i++) {
                if (n[i] > c[i]) return true;
                if (n[i] < c[i]) return false;
            }
            return false;
        } catch (Exception e) { return false; }
    }

    private static int[] parse(String v) {
        String[] parts = v.split("\\.");
        int[] r = new int[3];
        for (int i = 0; i < Math.min(3, parts.length); i++)
            r[i] = Integer.parseInt(parts[i].replaceAll("[^0-9]", ""));
        return r;
    }
}
