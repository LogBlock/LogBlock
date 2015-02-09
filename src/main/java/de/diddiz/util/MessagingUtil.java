package de.diddiz.util;

import de.diddiz.LogBlock.config.Config;
import org.bukkit.ChatColor;
import org.bukkit.Location;

public class MessagingUtil {

	public static final String DEFAULT = ChatColor.YELLOW.toString();

	public static String brackets(String string, BracketType type) {
		return ChatColor.DARK_GRAY + String.valueOf(type.getStarting()) + string + ChatColor.DARK_GRAY + type.getEnding() + DEFAULT;
	}

	public static String prettyDate(long date) {
		return ChatColor.DARK_AQUA + Config.formatter.format(date) + DEFAULT;
	}

	public static String prettyMaterial(String materialName) {
		return ChatColor.BLUE + materialName.toUpperCase() + DEFAULT;
	}

	public static String prettyLocation(Location loc) {
		return prettyLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
	}

	public static String prettyLocation(Number x, Number y, Number z) {
		return DEFAULT + "X: " + ChatColor.WHITE + x.intValue() + DEFAULT + ", Y: " + ChatColor.WHITE + y.intValue() + DEFAULT + ", Z: " + ChatColor.WHITE + z.intValue() + DEFAULT;
	}

	public enum BracketType {
		STANDARD('[', ']'),
		ANGLE('<', '>');

		private char starting, ending;

		BracketType(char starting, char ending) {
			this.starting = starting;
			this.ending = ending;
		}

		public char getStarting() {
			return starting;
		}

		public char getEnding() {
			return ending;
		}
	}
}
