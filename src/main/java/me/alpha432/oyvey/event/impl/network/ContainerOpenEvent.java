package me.alpha432.oyvey.event.impl.network;

import me.alpha432.oyvey.event.Event;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ContainerOpenEvent extends Event {
    private final Component title;
    private final List<ItemStack> items;

    public ContainerOpenEvent(Component title, List<ItemStack> items) {
        this.title = title;
        this.items = items;
    }

    public Component getTitle()      { return title; }
    public String    getTitleText()  { return title.getString(); }
    public List<ItemStack> getItems(){ return items; }
}
