package de.diddiz.LogBlock.componentwrapper;

import java.awt.Color;

public class ChatColor {
    public static final ChatColor BLACK = ChatColor.named("black");
    public static final ChatColor DARK_BLUE = ChatColor.named("dark_blue");
    public static final ChatColor DARK_GREEN = ChatColor.named("dark_green");
    public static final ChatColor DARK_AQUA = ChatColor.named("dark_aqua");
    public static final ChatColor DARK_RED = ChatColor.named("dark_red");
    public static final ChatColor DARK_PURPLE = ChatColor.named("dark_purple");
    public static final ChatColor GOLD = ChatColor.named("gold");
    public static final ChatColor GRAY = ChatColor.named("gray");
    public static final ChatColor DARK_GRAY = ChatColor.named("dark_gray");
    public static final ChatColor BLUE = ChatColor.named("blue");
    public static final ChatColor GREEN = ChatColor.named("green");
    public static final ChatColor AQUA = ChatColor.named("aqua");
    public static final ChatColor RED = ChatColor.named("red");
    public static final ChatColor LIGHT_PURPLE = ChatColor.named("light_purple");
    public static final ChatColor YELLOW = ChatColor.named("yellow");
    public static final ChatColor WHITE = ChatColor.named("white");

    private final String name;
    private final int color;

    public ChatColor(String name, int color) {
        this.name = name;
        this.color = color;
    }

    private static ChatColor named(String name) {
        return new ChatColor(name, 0);
    }

    public static ChatColor from(Color color) {
        return new ChatColor(null, color.getRGB() & 0xffffff);
    }

    public static ChatColor from(int color) {
        return new ChatColor(null, color & 0xffffff);
    }

    public boolean hasName() {
        return name != null;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }
}
