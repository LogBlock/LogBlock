package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;
import static de.diddiz.LogBlock.config.Config.isLogging;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.material.MaterialData;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;

public class BlockPlaceLogging extends LoggingListener
{
	public BlockPlaceLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockPlace(BlockPlaceEvent event) {
		final WorldConfig wcfg = getWorldConfig(event.getBlock().getWorld());
		if (!event.isCancelled() && wcfg != null && wcfg.isLogging(Logging.BLOCKPLACE)) {
			final int type = event.getBlock().getTypeId();
			final BlockState before = event.getBlockReplacedState();
			final BlockState after = event.getBlockPlaced().getState();
			final String playerName = event.getPlayer().getName();
			if (type == 0 && event.getItemInHand() != null) {
				if (event.getItemInHand().getTypeId() == 51)
					return;
				after.setTypeId(event.getItemInHand().getTypeId());
				after.setData(new MaterialData(event.getItemInHand().getTypeId()));
			}
			// Delay queuing of stairs and blocks by 1 tick to allow the raw data to update 
			if (type == 53 || type == 67 || type == 108 || type == 109 || type == 114 || type == 128 || type == 134 || type == 135 || type == 136 || type == 26) {
				LogBlock.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(LogBlock.getInstance(), new Runnable() {
					
					@Override
					public void run() {
						if (before.getTypeId() == 0)
							consumer.queueBlockPlace(playerName, after);
						else
							consumer.queueBlockReplace(playerName, before, after);
					}
					
				}, 1L);
				return;
			}
			if (wcfg.isLogging(Logging.SIGNTEXT) && (type == 63 || type == 68))
				return;
			if (before.getTypeId() == 0)
				consumer.queueBlockPlace(event.getPlayer().getName(), after);
			else
				consumer.queueBlockReplace(event.getPlayer().getName(), before, after);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
		if (!event.isCancelled() && isLogging(event.getPlayer().getWorld(), Logging.BLOCKPLACE))
			consumer.queueBlockPlace(event.getPlayer().getName(), event.getBlockClicked().getRelative(event.getBlockFace()).getLocation(), event.getBucket() == Material.WATER_BUCKET ? 9 : 11, (byte)0);
	}
}
