package me.alpha432.oyvey.features.modules.hud;

import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.client.HudModule;

public class BattleHudModule extends HudModule {

    public BattleHudModule() {
        super("BattleHud", "Overlay shown during Cobblemon battles", 200, 60);
        showDuringBattle = true;
    }

    @Override
    protected void render(Render2DEvent e) {
        super.render(e); // HUD editor drag support
        // battle-specific UI goes here
    }
}
