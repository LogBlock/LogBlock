package de.diddiz.LogBlock.componentwrapper;

import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.platform.PlatformAdapter;
import org.bukkit.command.CommandSender;

public class Components {
    private static final Component EMPTY = text("");
    private static final Component SPACE = text(" ");

    public static Component empty() {
        return EMPTY;
    }

    public static Component space() {
        return SPACE;
    }

    public static TextComponent text(String text) {
        return text(text, null);
    }

    public static TextComponent text(String text, ChatColor color, Component... children) {
        return text(text, color, null, null, children);
    }

    public static TextComponent text(String text, ChatColor color, Hover hover, Click click, Component... children) {
        return new TextComponent(text, color, hover, click, children);
    }

    private static PlatformAdapter adapter() {
        return LogBlock.getInstance().getPlatformAdapter();
    }

    public static void sendTo(CommandSender target, Component message) {
        adapter().sendComponent(target, message);
    }

    public static Component fromLegacy(String text) {
        return adapter().fromLegacy(text);
    }

    public static String toPlainText(Component component) {
        return adapter().toPlainText(component);
    }
}
