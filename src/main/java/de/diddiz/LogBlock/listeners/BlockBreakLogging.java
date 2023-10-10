package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;
import de.diddiz.LogBlock.util.BukkitUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;
import static de.diddiz.LogBlock.config.Config.isLogging;
import static de.diddiz.LogBlock.util.LoggingUtil.smartLogBlockBreak;
import static de.diddiz.LogBlock.util.LoggingUtil.smartLogBlockReplace;
import static de.diddiz.LogBlock.util.LoggingUtil.smartLogFallables;

public class BlockBreakLogging extends LoggingListener {
    public BlockBreakLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isLogging(event.getBlock().getWorld(), Logging.BLOCKBREAK)) {
            WorldConfig wcfg = getWorldConfig(event.getBlock().getWorld());
            if (wcfg == null) {
                return;
            }

            final Actor actor = Actor.actorFromEntity(event.getPlayer());
            final Block origin = event.getBlock();
            final Material type = origin.getType();

            if (wcfg.isLogging(Logging.CHESTACCESS) && BukkitUtils.isContainerBlock(type) && !BukkitUtils.isShulkerBoxBlock(type)) {
                consumer.queueContainerBreak(actor, origin.getState());
            } else if (type == Material.ICE) {
                // When in creative mode ice doesn't form water
                if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
                    smartLogBlockBreak(consumer, actor, origin);
                } else {
                    smartLogBlockReplace(consumer, actor, origin, Bukkit.createBlockData(Material.WATER));
                }
            } else {
                smartLogBlockBreak(consumer, actor, origin);
            }
            smartLogFallables(consumer, actor, origin);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        if (isLogging(event.getBlockClicked().getWorld(), Logging.BLOCKBREAK)) {
            BlockData clickedBlockData = event.getBlockClicked().getBlockData();
            if (clickedBlockData instanceof Waterlogged) {
                Waterlogged clickedWaterlogged = (Waterlogged) clickedBlockData;
                if (clickedWaterlogged.isWaterlogged()) {
                    Waterlogged clickedWaterloggedWithoutWater = (Waterlogged) clickedWaterlogged.clone();
                    clickedWaterloggedWithoutWater.setWaterlogged(false);
                    consumer.queueBlockReplace(Actor.actorFromEntity(event.getPlayer()), event.getBlockClicked().getLocation(), clickedWaterlogged, clickedWaterloggedWithoutWater);
                }
            } else {
                consumer.queueBlockBreak(Actor.actorFromEntity(event.getPlayer()), event.getBlockClicked().getState());
            }
        }
    }
}
