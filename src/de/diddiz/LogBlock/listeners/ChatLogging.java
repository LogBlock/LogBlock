package de.diddiz.LogBlock.listeners;

import com.dthielke.herochat.ChannelChatEvent;
import static de.diddiz.LogBlock.config.Config.isLogging;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;

public class ChatLogging extends LoggingListener
{
	public ChatLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (isLogging(event.getPlayer().getWorld(), Logging.CHAT))
			consumer.queueChat(event.getPlayer().getName(), event.getMessage());
	}
        
        @EventHandler(priority = EventPriority.MONITOR)
        public void onChannelChatEvent(ChannelChatEvent event) {
            PlayerChatEvent bukkitEvent = event.getBukkitEvent();
            if (isLogging(bukkitEvent.getPlayer().getWorld(), Logging.CHAT))
			consumer.queueChat(bukkitEvent.getPlayer().getName(), bukkitEvent.getMessage());
        }

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerChat(PlayerChatEvent event) {
		if (isLogging(event.getPlayer().getWorld(), Logging.CHAT))
			consumer.queueChat(event.getPlayer().getName(), event.getMessage());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onServerCommand(ServerCommandEvent event) {
		consumer.queueChat("Console", "/" + event.getCommand());
	}
}
