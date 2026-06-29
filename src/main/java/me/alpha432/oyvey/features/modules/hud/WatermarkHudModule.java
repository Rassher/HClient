package me.alpha432.oyvey.features.modules.hud;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.client.ClickGuiModule;
import me.alpha432.oyvey.features.modules.client.HudModule;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.BuildConfig;
import me.alpha432.oyvey.util.ColorUtil;

public class WatermarkHudModule extends HudModule {
    public final Setting<String> text = str("Text", "HClient");
    public final Setting<Boolean> fullVersion = new Setting<>("FullVersion", false);

    public WatermarkHudModule() {
        super("Watermark", "Display watermark", 100, 10);
        if (BuildConfig.USING_GIT) {
            register(fullVersion);
        }
    }

    @Override
    protected void render(Render2DEvent e) {
        super.render(e);
        String name = BuildConfig.NAME + " " + BuildConfig.VERSION;
        if (fullVersion.getValue() && BuildConfig.USING_GIT) {
            name += "/" + BuildConfig.BRANCH + "-" + BuildConfig.HASH;
        }

        boolean rainbow = ClickGuiModule.getInstance().rainbow.getValue();
        float x = getX();
        float y = getY();

        if (rainbow) {
            // Draw each character with a shifted hue for a smooth gradient
            for (int i = 0; i < name.length(); i++) {
                String ch = String.valueOf(name.charAt(i));
                int color = ColorUtil.rainbowVivid((int)((x + y) * 12) + i * 200).getRGB();
                e.getContext().drawString(mc.font, ch, (int) x, (int) y, color);
                x += mc.font.width(ch);
            }
        } else {
            e.getContext().drawString(mc.font, name, (int) x, (int) y,
                    OyVey.colorManager.getColorAsInt());
        }

        setWidth(mc.font.width(name));
        setHeight(mc.font.lineHeight);
    }
}
