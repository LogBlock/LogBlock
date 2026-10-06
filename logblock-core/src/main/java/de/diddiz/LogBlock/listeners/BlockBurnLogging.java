package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.Config;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.player.PlayerInteractEvent;

import static de.diddiz.LogBlock.config.Config.isLogging;
import static de.diddiz.LogBlock.util.LoggingUtil.smartLogBlockBreak;
import static de.diddiz.LogBlock.util.LoggingUtil.smartLogBlockReplace;
import static de.diddiz.LogBlock.util.LoggingUtil.smartLogFallables;

public class BlockBurnLogging extends LoggingListener {
    public BlockBurnLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (isLogging(event.getBlock().getWorld(), Logging.FIRE)) {
            smartLogBlockReplace(consumer, new Actor("Fire", Config.logFireSpreadAsPlayerWhoCreatedIt ? event.getIgnitingBlock() : null), event.getBlock(), Material.FIRE.createBlockData());
            smartLogFallables(consumer, new Actor("Fire", Config.logFireSpreadAsPlayerWhoCreatedIt ? event.getIgnitingBlock() : null), event.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        Actor actor = new Actor("Fire", Config.logFireSpreadAsPlayerWhoCreatedIt ? event.getIgnitingBlock() : null);
        if (event.getCause() == IgniteCause.FLINT_AND_STEEL) {
            if (event.getIgnitingEntity() != null) {
                return; // handled in block place
            } else {
                actor = new Actor("Dispenser");
            }
        } else if (event.getCause() == IgniteCause.LIGHTNING) {
            actor = new Actor("Lightning");
        } else if (event.getCause() == IgniteCause.EXPLOSION) {
            actor = new Actor("Explosion");
        } else if (event.getCause() == IgniteCause.LAVA) {
            actor = new Actor("Lava");
        } else if (event.getCause() == IgniteCause.ENDER_CRYSTAL) {
            actor = new Actor("EnderCrystal");
        }
        if (isLogging(event.getBlock().getWorld(), Logging.FIRE)) {
            consumer.queueBlockPlace(actor, event.getBlock().getLocation(), Material.FIRE.createBlockData());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExtinguish(PlayerInteractEvent event) {
        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
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
