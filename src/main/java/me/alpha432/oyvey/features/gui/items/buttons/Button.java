package me.alpha432.oyvey.features.gui.items.buttons;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.gui.OyVeyGui;
import me.alpha432.oyvey.features.gui.Widget;
import me.alpha432.oyvey.features.gui.items.Item;
import me.alpha432.oyvey.features.modules.client.ClickGuiModule;
import me.alpha432.oyvey.util.ColorUtil;
import me.alpha432.oyvey.util.render.RenderUtil;


import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class Button
        extends Item {
    private boolean state;

    public Button(String name) {
        super(name);
        this.height = 15;
    }

    @Override
    public void drawScreen(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
        boolean rainbowOn = ClickGuiModule.getInstance().rainbow.getValue();
        int btnColor;
        if (this.getState()) {
            if (rainbowOn) {
                // Wave: each button offset by Y position so the wave flows top→bottom within each column
                btnColor = ColorUtil.rainbow((int)((this.x + this.y) * 12)).getRGB();
            } else {
                btnColor = this.isHovering(mouseX, mouseY)
                    ? OyVey.colorManager.getColorWithAlpha(y, ClickGuiModule.getInstance().topColor.getValue().getAlpha())
                    : OyVey.colorManager.getColorWithAlpha(y, ClickGuiModule.getInstance().color.getValue().getAlpha());
            }
        } else {
            btnColor = this.isHovering(mouseX, mouseY) ? -2007673515 : 0x11555555;
        }
        RenderUtil.rect(context, this.x, this.y, this.x + (float) this.width, this.y + (float) this.height - 0.5f, btnColor);
        drawString(this.getName(), this.x + 2.3f, this.y - 2.0f - (float) OyVeyGui.getClickGui().getTextOffset(), this.getState() ? -1 : -5592406);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
            this.onMouseClick();
        }
    }

    public void onMouseClick() {
        this.state = !this.state;
        this.toggle();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    public void toggle() {
    }

    public boolean getState() {
        return this.state;
    }

    @Override
    public int getHeight() {
        return 14;
    }

    protected int activeColor(float x, float y, int alpha) {
        if (ClickGuiModule.getInstance().rainbow.getValue()) {
            return ColorUtil.rainbow((int)((x + y) * 12)).getRGB();
        }
        return OyVey.colorManager.getColorWithAlpha(y, alpha);
    }

    public boolean isHovering(int mouseX, int mouseY) {
        for (Widget widget : OyVeyGui.getClickGui().getComponents()) {
            if (!widget.drag) continue;
            return false;
        }
        return (float) mouseX >= this.getX() && (float) mouseX <= this.getX() + (float) this.getWidth() && (float) mouseY >= this.getY() && (float) mouseY < this.getY() + (float) this.height;
    }
}