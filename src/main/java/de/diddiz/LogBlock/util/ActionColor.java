package de.diddiz.LogBlock.util;

import de.diddiz.LogBlock.componentwrapper.ChatColor;

public enum ActionColor {
    DESTROY(ChatColor.RED),
    CREATE(ChatColor.DARK_GREEN),
    INTERACT(ChatColor.GRAY);

    private final ChatColor color;

    ActionColor(ChatColor color) {
        this.color = color;
    }

    public ChatColor getColor() {
        return color;
    }

    @Override
    public String toString() {
        return color.toString();
    }
}
