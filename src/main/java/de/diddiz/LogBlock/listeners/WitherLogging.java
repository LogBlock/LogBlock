package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.isLogging;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;

public class WitherLogging extends LoggingListener
{
	public WitherLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityChangeBlock(EntityChangeBlockEvent event) {
		if (event.getEntity() instanceof Wither && isLogging(event.getBlock().getWorld(), Logging.WITHER))
			consumer.queueBlockReplace("Wither", event.getBlock().getState(), event.getTo().getId(), (byte)0); // Wither walked through a block.
	}
}
