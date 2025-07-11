package de.diddiz.LogBlock.componentwrapper;

import org.bukkit.inventory.ItemStack;

public interface Hover {
    public static TextHover text(String text) {
        return new TextHover(Components.text(text));
    }

    public static TextHover text(Component text) {
        return new TextHover(text);
    }

    public static ItemHover item(ItemStack item) {
        return new ItemHover(item);
    }
}
