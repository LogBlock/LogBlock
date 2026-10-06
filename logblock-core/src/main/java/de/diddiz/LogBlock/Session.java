package de.diddiz.LogBlock;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

import static de.diddiz.LogBlock.config.Config.toolsByType;
import static org.bukkit.Bukkit.getServer;

public class Session {
    private static final Map<String, Session> sessions = new HashMap<>();
    public QueryParams lastQuery = null;
    public LookupCacheElement[] lookupCache = null;
    public int page = 1;
    public Map<Tool, ToolData> toolData;

    private Session(Player player) {
        toolData = new HashMap<>();
        final LogBlock logblock = LogBlock.getInstance();
        if (player != null) {
            for (final Tool tool : toolsByType.values()) {
                toolData.put(tool, new ToolData(tool, logblock, player));
            }
        }
    }

    public static boolean hasSession(CommandSender sender) {
        return sessions.containsKey(sender.getName().toLowerCase());
    }

    public static boolean hasSession(String playerName) {
        return sessions.containsKey(playerName.toLowerCase());
    }

    public static Session getSession(CommandSender sender) {
        return getSession(sender.getName());
    }

    public static Session getSession(String playerName) {
        Session session = sessions.get(playerName.toLowerCase());
        if (session == null) {
            session = new Session(getServer().getPlayer(playerName));
            sessions.put(playerName.toLowerCase(), session);
        }
        return session;
    }
}
