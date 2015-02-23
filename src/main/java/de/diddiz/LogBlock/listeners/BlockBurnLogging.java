package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.isLogging;
import static de.diddiz.util.LoggingUtil.smartLogBlockBreak;
import static de.diddiz.util.LoggingUtil.smartLogFallables;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;

public class BlockBurnLogging extends LoggingListener
{
	public BlockBurnLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		if (isLogging(event.getBlock().getWorld(), Logging.FIRE)) {
			smartLogBlockBreak(consumer, new Actor("Fire"), event.getBlock());
			smartLogFallables(consumer, new Actor("Fire"), event.getBlock());
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onExtinguish(PlayerInteractEvent event) {
		if(event.getAction().equals(Action.LEFT_CLICK_BLOCK)){
			Player player = event.getPlayer();
			Block block = event.getClickedBlock().getRelative(event.getBlockFace());
			if (block.getType().equals(Material.FIRE) && isLogging(player.getWorld(), Logging.FIRE)) {
				Actor actor = Actor.actorFromEntity(player);
				smartLogBlockBreak(consumer, actor, block);
				smartLogFallables(consumer, actor, block);
			}
		}
	}
}
