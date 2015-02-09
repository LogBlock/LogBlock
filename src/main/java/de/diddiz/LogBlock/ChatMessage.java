package de.diddiz.LogBlock;

import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;

import static de.diddiz.util.ActionColor.CREATE;
import static de.diddiz.util.MessagingUtil.*;

public class ChatMessage implements LookupCacheElement
{
	final long id, date;
	final String playerName, message;

	public ChatMessage(String playerName, String message) {
		id = 0;
		date = System.currentTimeMillis() / 1000;
		this.playerName = playerName;
		this.message = message;
	}

	public ChatMessage(ResultSet rs, QueryParams p) throws SQLException {
		id = p.needId ? rs.getInt("id") : 0;
		date = p.needDate ? rs.getTimestamp("date").getTime() : 0;
		playerName = p.needPlayer ? rs.getString("playername") : null;
		message = p.needMessage ? rs.getString("message") : null;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getMessage() {
		return (playerName != null ? brackets(ChatColor.WHITE + playerName, BracketType.ANGLE) + ' ' : "") + (message != null ? CREATE + message : "");
	}
}
