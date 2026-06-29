package me.alpha432.oyvey.features.gui;

import com.google.gson.*;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class HClientConfig {

    public static final HClientConfig INSTANCE = new HClientConfig();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private String  background         = "RGB Particles";
    private boolean suppressRiskWarning = false;

    private HClientConfig() {}

    public String  getBackground()                    { return background; }
    public void    setBackground(String name)          { background = name; save(); }
    public boolean isSuppressRiskWarning()             { return suppressRiskWarning; }
    public void    setSuppressRiskWarning(boolean val) { suppressRiskWarning = val; save(); }

    public void load() {
        try {
            Path path = getPath();
            if (!path.toFile().exists()) return;
            JsonObject obj = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if (obj.has("background"))          background          = obj.get("background").getAsString();
            if (obj.has("suppressRiskWarning")) suppressRiskWarning = obj.get("suppressRiskWarning").getAsBoolean();
        } catch (Exception ignored) {}
    }

    public void save() {
        try {
            Path dir = getDir();
            if (!dir.toFile().exists()) dir.toFile().mkdirs();
            JsonObject obj = new JsonObject();
            obj.addProperty("background",          background);
            obj.addProperty("suppressRiskWarning", suppressRiskWarning);
            Files.writeString(getPath(), GSON.toJson(obj));
        } catch (Exception ignored) {}
    }

    private Path getDir()  { return Minecraft.getInstance().gameDirectory.toPath().resolve("hclient"); }
    private Path getPath() { return getDir().resolve("ui.json"); }
}
