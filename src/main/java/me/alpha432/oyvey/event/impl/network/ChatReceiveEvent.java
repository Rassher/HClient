package me.alpha432.oyvey.event.impl.network;

import me.alpha432.oyvey.event.Event;
import net.minecraft.network.chat.Component;

public class ChatReceiveEvent extends Event {
    private final Component component;
    private final boolean overlay; // true = action bar

    public ChatReceiveEvent(Component component, boolean overlay) {
        this.component = component;
        this.overlay   = overlay;
    }

    public Component getComponent() { return component; }
    public String    getMessage()   { return component.getString(); }
    public boolean   isOverlay()    { return overlay; }
}
