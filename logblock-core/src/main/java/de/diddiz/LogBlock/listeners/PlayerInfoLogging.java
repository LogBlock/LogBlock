package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.config.Config;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerInfoLogging extends LoggingListener {

    private final HashMap<UUID, Long> playerLogins = new HashMap<>();

    public PlayerInfoLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerLogins.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        consumer.queueJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        onPlayerQuit(event.getPlayer());
    }

    public void onPlayerQuit(Player player) {
        Long joinTime = playerLogins.remove(player.getUniqueId());
        if (Config.logPlayerInfo && joinTime != null) {
            long onlineTime = (System.currentTimeMillis() - joinTime) / 1000;
            if (onlineTime > 0) {
                consumer.queueLeave(player, onlineTime);
            }
        }
    }
}
