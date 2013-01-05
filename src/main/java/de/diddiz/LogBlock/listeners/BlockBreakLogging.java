package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;
import static de.diddiz.LogBlock.config.Config.isLogging;
import static de.diddiz.util.LoggingUtil.smartLogBlockBreak;
import static de.diddiz.util.LoggingUtil.smartLogFallables;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;

public class BlockBreakLogging extends LoggingListener {

	public BlockBreakLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (isLogging(event.getBlock().getWorld(), Logging.BLOCKBREAK)) {
			WorldConfig wcfg = getWorldConfig(event.getBlock().getWorld());
			if (wcfg == null) return;

			final String playerName = event.getPlayer().getName();
			final Block origin = event.getBlock();
			final int type = origin.getTypeId();

			if (wcfg.isLogging(Logging.SIGNTEXT) && (type == 63 || type == 68)) {
				consumer.queueSignBreak(playerName, (Sign) origin.getState());
			} else if (wcfg.isLogging(Logging.CHESTACCESS) && (type == 23 || type == 54 || type == 61)) {
				consumer.queueContainerBreak(playerName, origin.getState());
			} else if (origin.getType() == Material.ICE) {
				// When in creative mode ice doesn't form water
				if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
					consumer.queueBlockBreak(playerName, origin.getState());
				} else {
					consumer.queueBlockReplace(playerName, origin.getState(), 9, (byte) 0);
				}
			} else {
				smartLogBlockBreak(consumer, playerName, origin);
			}
			smartLogFallables(consumer, playerName, origin);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerBucketFill(PlayerBucketFillEvent event) {
		if (isLogging(event.getBlockClicked().getWorld(), Logging.BLOCKBREAK)) {
			consumer.queueBlockBreak(event.getPlayer().getName(), event.getBlockClicked().getState());
		}
	}
}
