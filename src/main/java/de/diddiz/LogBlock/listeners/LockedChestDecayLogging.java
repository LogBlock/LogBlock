package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.isLogging;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockFadeEvent;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;

public class LockedChestDecayLogging extends LoggingListener
{
	public LockedChestDecayLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockFade(BlockFadeEvent event) {
		if (isLogging(event.getBlock().getWorld(), Logging.LOCKEDCHESTDECAY)) {
			final int type = event.getBlock().getTypeId();
			if (type == 95)
				consumer.queueBlockReplace("LockedChestDecay", event.getBlock().getState(), event.getNewState());
		}
	}
}
