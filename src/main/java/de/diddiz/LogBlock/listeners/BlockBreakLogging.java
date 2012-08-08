package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;
import static de.diddiz.LogBlock.config.Config.isLogging;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;
import de.diddiz.util.BukkitUtils;

public class BlockBreakLogging extends LoggingListener
{
	public BlockBreakLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		final WorldConfig wcfg = getWorldConfig(event.getBlock().getWorld());
		if (wcfg != null && wcfg.isLogging(Logging.BLOCKBREAK)) {
			final int type = event.getBlock().getTypeId();
			if (wcfg.isLogging(Logging.SIGNTEXT) && (type == 63 || type == 68))
				consumer.queueSignBreak(event.getPlayer().getName(), (Sign)event.getBlock().getState());
			else if (wcfg.isLogging(Logging.CHESTACCESS) && (type == 23 || type == 54 || type == 61))
				consumer.queueContainerBreak(event.getPlayer().getName(), event.getBlock().getState());
			else if (type == 79)
				consumer.queueBlockReplace(event.getPlayer().getName(), event.getBlock().getState(), 9, (byte)0);
			else {
				consumer.queueBlockBreak(event.getPlayer().getName(), event.getBlock().getState());
				List<Location> nearbySigns = BukkitUtils.getBlocksNearby(event.getBlock(), BukkitUtils.getRelativeBreakables());
				if(nearbySigns.size() != 0) {
					for(Location location : nearbySigns) {
						int blockType = location.getBlock().getTypeId();
						if (wcfg.isLogging(Logging.SIGNTEXT) && (blockType == 63 || type == 68))
							consumer.queueSignBreak(event.getPlayer().getName(), (Sign) location.getBlock().getState());
						consumer.queueBlockBreak(event.getPlayer().getName(), location.getBlock().getState());
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerBucketFill(PlayerBucketFillEvent event) {
		if (isLogging(event.getPlayer().getWorld(), Logging.BLOCKBREAK))
			consumer.queueBlockBreak(event.getPlayer().getName(), event.getBlockClicked().getState());
	}
}
