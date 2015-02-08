package de.diddiz.LogBlock;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.Location;

public class ChatMessage extends AbstractLookupCacheElement
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
		return (playerName != null ? "<" + playerName + "> " : "") + (message != null ? message : "");
	}
}
