package de.diddiz.LogBlock;

import static de.diddiz.LogBlock.util.LoggingUtil.checkText;
import static de.diddiz.LogBlock.util.MessagingUtil.brackets;
import static de.diddiz.LogBlock.util.MessagingUtil.prettyDate;

import de.diddiz.LogBlock.componentwrapper.ChatColor;
import de.diddiz.LogBlock.componentwrapper.Component;
import de.diddiz.LogBlock.componentwrapper.Components;
import de.diddiz.LogBlock.util.MessagingUtil;
import de.diddiz.LogBlock.util.MessagingUtil.BracketType;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.Location;

public class ChatMessage implements LookupCacheElement {
    final long id, date;
    final String playerName, message;
    final Actor player;

    public ChatMessage(Actor player, String message) {
        id = 0;
        date = System.currentTimeMillis() / 1000;
        this.player = player;
        this.message = checkText(message);
        this.playerName = player == null ? null : player.getName();
    }

    public ChatMessage(ResultSet rs, QueryParams p) throws SQLException {
        id = p.needId ? rs.getLong("id") : 0;
        date = p.needDate ? rs.getTimestamp("date").getTime() : 0;
        player = p.needPlayer ? new Actor(rs) : null;
        playerName = p.needPlayer ? rs.getString("playername") : null;
        message = p.needMessage ? rs.getString("message") : null;
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public Component getLogMessage(int entry) {
        Component msg = Components.empty();
        if (date > 0) {
            msg = msg.append(prettyDate(date));
            msg = msg.append(" ");
        }
        if (playerName != null) {
            msg = msg.append(brackets(BracketType.ANGLE, MessagingUtil.createTextComponentWithColor(playerName, ChatColor.WHITE)));
            msg = msg.append(" ");
        }
        if (message != null) {
            msg = msg.append(Components.fromLegacy(message));
        }
        return msg;
    }
}
