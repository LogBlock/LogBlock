package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.isLogging;
import com.dthielke.herochat.ChannelChatEvent;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChatEvent;

public class ChannelChatLogging extends LoggingListener {
    
    public ChannelChatLogging(LogBlock lb) {
        super(lb);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
        public void onChannelChatEvent(ChannelChatEvent event) {
            PlayerChatEvent bukkitEvent = event.getBukkitEvent();
            if (isLogging(bukkitEvent.getPlayer().getWorld(), Logging.CHAT))
			consumer.queueChat(bukkitEvent.getPlayer().getName(), bukkitEvent.getMessage());
        }
}