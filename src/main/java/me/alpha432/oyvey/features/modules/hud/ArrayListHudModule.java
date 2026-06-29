package me.alpha432.oyvey.features.modules.hud;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.client.ClickGuiModule;
import me.alpha432.oyvey.features.modules.client.HudModule;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.ColorUtil;
import net.minecraft.ChatFormatting;

import java.util.Comparator;
import java.util.List;

public class ArrayListHudModule extends HudModule {
    public final Setting<Boolean> rainbow = bool("Rainbow", true);
    public final Setting<Boolean> showCategory = bool("ShowCategory", false);

    public ArrayListHudModule() {
        super("ArrayList", "Shows active modules", 80, 10);
    }

    @Override
    protected void render(Render2DEvent e) {
        super.render(e);
        if (nullCheck()) return;

        List<Module> active = OyVey.moduleManager.stream()
                .filter(m -> m.isEnabled()
                        && m.getCategory() != Module.Category.HUD
                        && m.getCategory() != Module.Category.CLIENT)
                .sorted(Comparator.comparingInt(m -> -mc.font.width(getLabel(m))))
                .toList();

        if (active.isEmpty()) {
            setWidth(80);
            setHeight(mc.font.lineHeight);
            return;
        }

        float x = getX();
        float startY = getY();
        int lineH = mc.font.lineHeight + 1;
        float maxWidth = 0;

        for (int i = 0; i < active.size(); i++) {
            Module m = active.get(i);
            String label = getLabel(m);
            float textWidth = mc.font.width(label);
            if (textWidth > maxWidth) maxWidth = textWidth;

            int color;
            if (rainbow.getValue() && ClickGuiModule.getInstance().rainbow.getValue()) {
                color = ColorUtil.rainbowVivid((int)((x + startY + i * lineH) * 12)).getRGB();
            } else {
                color = OyVey.colorManager.getColorAsInt();
            }

            e.getContext().drawString(mc.font, label, (int) x, (int)(startY + i * lineH), color);
        }

        setWidth(maxWidth);
        setHeight(active.size() * lineH);
    }

    private String getLabel(Module m) {
        String info = m.getDisplayInfo();
        String name = m.getName();
        if (showCategory.getValue()) name += " " + ChatFormatting.DARK_GRAY + m.getCategory().getName();
        if (info != null && !info.isEmpty()) name += " " + ChatFormatting.GRAY + info;
        return name;
    }
}
