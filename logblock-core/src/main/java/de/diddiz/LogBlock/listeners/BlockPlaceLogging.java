package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.Config;
import de.diddiz.LogBlock.util.LoggingUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

import static de.diddiz.LogBlock.config.Config.isLogging;

public class BlockPlaceLogging extends LoggingListener {
    public BlockPlaceLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (Config.isLogging(event.getBlock().getWorld(), Logging.BLOCKPLACE)) {
            final BlockState before = event.getBlockReplacedState();
            final BlockState after = event.getBlockPlaced().getState();
            final Actor actor = Actor.actorFromEntity(event.getPlayer());
            if (before.getType() == Material.LECTERN && after.getType() == Material.LECTERN) {
                return;
            }
            LoggingUtil.smartLogBlockPlace(consumer, actor, before, after);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (isLogging(event.getPlayer().getWorld(), Logging.BLOCKPLACE)) {
            Material placedMaterial = event.getBucket() == Material.LAVA_BUCKET ? Material.LAVA : Material.WATER;
            BlockData clickedBlockData = event.getBlockClicked().getBlockData();
            if (placedMaterial == Material.WATER && clickedBlockData instanceof Waterlogged) {
                Waterlogged clickedWaterlogged = (Waterlogged) clickedBlockData;
                if (!clickedWaterlogged.isWaterlogged()) {
                    Waterlogged clickedWaterloggedWithWater = (Waterlogged) clickedWaterlogged.clone();
                    clickedWaterloggedWithWater.setWaterlogged(true);
                    consumer.queueBlockReplace(Actor.actorFromEntity(event.getPlayer()), event.getBlockClicked().getLocation(), clickedWaterlogged, clickedWaterloggedWithWater);
                    return;
                }
            }
            Block placedAt = event.getBlockClicked().getRelative(event.getBlockFace());
            if (placedAt.isEmpty()) {
                consumer.queueBlockPlace(Actor.actorFromEntity(event.getPlayer()), placedAt.getLocation(), placedMaterial.createBlockData());
            } else {
                BlockData placedAtBlock = placedAt.getBlockData();
                if (placedAtBlock instanceof Waterlogged && !(((Waterlogged) placedAtBlock).isWaterlogged())) {
                    Waterlogged clickedWaterloggedWithWater = (Waterlogged) placedAtBlock.clone();
                    clickedWaterloggedWithWater.setWaterlogged(true);
                    consumer.queueBlockReplace(Actor.actorFromEntity(event.getPlayer()), placedAt.getLocation(), placedAtBlock, clickedWaterloggedWithWater);
                } else {
                    consumer.queueBlockReplace(Actor.actorFromEntity(event.getPlayer()), placedAt.getLocation(), placedAtBlock, placedMaterial.createBlockData());
                }
            }
        }
    }
}
