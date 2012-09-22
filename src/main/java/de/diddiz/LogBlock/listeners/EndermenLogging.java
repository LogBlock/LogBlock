package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.isLogging;
import org.bukkit.entity.Enderman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;

public class EndermenLogging extends LoggingListener
{
	public EndermenLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityChangeBlock(EntityChangeBlockEvent event) {
		if (event.getEntity() instanceof Enderman && isLogging(event.getBlock().getWorld(), Logging.ENDERMEN)) {
			Enderman enderman = (Enderman) event.getEntity();
			consumer.queueBlockReplace("Enderman", event.getBlock().getState(), event.getTo().getId(), enderman.getCarriedMaterial().getData());
		}
	}
}
