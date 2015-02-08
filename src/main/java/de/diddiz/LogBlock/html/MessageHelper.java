package de.diddiz.LogBlock.html;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static de.diddiz.LogBlock.html.Parser.escape;

public class MessageHelper {
	private static final String PLAYER_FORMAT = "<span onClick=\"suggest_command('/pm %1$s ')\"%3$s>%2$s</span>";

	public static final String ONLINE_COLOR = "dark_green";
	public static final String OFFLINE_COLOR = "dark_red";

	public static String format(CommandSender commandSender) {
		return format(commandSender.getName(), commandSender);
	}

	public static String format(String name) {
		return format(name, Bukkit.getPlayerExact(name));
	}

	private static String format(String name, CommandSender commandSender) {
		final String onHover;
		String displayName;
		if (commandSender == null) {
			onHover = "";
			displayName = name;
		}
		else {
			if (commandSender instanceof Player) {
				displayName = ((Player) commandSender).getDisplayName();
			}
			else {
				displayName = commandSender.getName();
			}
			if (commandSender instanceof Player) {
				final Player player = (Player) commandSender;
				final String color = player.isOnline() ? ONLINE_COLOR : OFFLINE_COLOR;
				final String hoverText = String.format("<color name=\"%1$s\">%2$s</color>", color, name);
				onHover = " onHover=\"show_text('" + escape(hoverText) + "')\"";
			}
			else {
				onHover = "";
			}
		}
		return String.format(PLAYER_FORMAT, name, displayName, onHover);
	}

	public static String button(String command, String label, String color, boolean run) {
		final String eventType = run ? "run_command" : "suggest_command";
		return String.format("<color name=\"%3$s\" onClick=\"%4$s('%1$s')\" onHover=\"show_text('%1$s')\">[%2$s]</color>", escape(command), escape(label), escape(color), eventType);
	}

	public static void sendServerMessage(String format, Object... params) {
		sendServerMessage(Predicates.alwaysTrue(), format, params);
	}

	public static void sendServerMessage(Predicate<? super Player> predicate, String format, Object... params) {
		final Player[] players = Bukkit.getOnlinePlayers();
		final List<CommandSender> targetPlayers = new ArrayList<CommandSender>();

		for (Player player : players) {
			if (!predicate.apply(player))
				continue;

			targetPlayers.add(player);
		}

		Parser.sendToPlayers(targetPlayers, format, params);
	}

	public static void sendMessage(CommandSender commandSender, String format, Object... params) {
		Parser.sendToPlayer(commandSender, format, params);
	}
}
