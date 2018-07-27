package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;
import de.diddiz.util.BukkitUtils;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockFromToEvent;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;

public class FluidFlowLogging extends LoggingListener {

    public FluidFlowLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        final WorldConfig wcfg = getWorldConfig(event.getBlock().getWorld());
        if (wcfg != null) {
            final BlockData blockDataFrom = event.getBlock().getBlockData();
            final Material typeFrom = blockDataFrom.getMaterial();
            
            final Block to = event.getToBlock();
            final Material typeTo = to.getType();
            final boolean canFlow = BukkitUtils.isEmpty(typeTo) || BukkitUtils.getNonFluidProofBlocks().contains(typeTo);
            if (typeFrom == Material.LAVA) {
                Levelled levelledFrom = (Levelled)blockDataFrom;
                if (canFlow && wcfg.isLogging(Logging.LAVAFLOW)) {
                    if (isSurroundedByWater(to) && levelledFrom.getLevel() <= 2) {
                        consumer.queueBlockReplace(new Actor("LavaFlow"), to.getState(), Material.COBBLESTONE.createBlockData());
                    } else {
                        Levelled newBlock = (Levelled) blockDataFrom.clone();
                        newBlock.setLevel(levelledFrom.getLevel() + 1);
                        if (BukkitUtils.isEmpty(typeTo)) {
                            consumer.queueBlockPlace(new Actor("LavaFlow"), to.getLocation(), newBlock);
                        } else {
                            consumer.queueBlockReplace(new Actor("LavaFlow"), to.getState(), newBlock);
                        }
                    }
                } else if (typeTo == Material.WATER) {
                    if (event.getFace() == BlockFace.DOWN) {
                        consumer.queueBlockReplace(new Actor("LavaFlow"), to.getState(), Material.STONE.createBlockData());
                    } else {
                        consumer.queueBlockReplace(new Actor("LavaFlow"), to.getState(), Material.COBBLESTONE.createBlockData());
                    }
                }
            } else if ((typeFrom == Material.WATER) && wcfg.isLogging(Logging.WATERFLOW)) {
                Levelled levelledFrom = (Levelled)blockDataFrom;
                Levelled newBlock = (Levelled) blockDataFrom.clone();
                newBlock.setLevel(levelledFrom.getLevel() + 1);
                if (BukkitUtils.isEmpty(typeTo)) {
                    consumer.queueBlockPlace(new Actor("WaterFlow"), to.getLocation(), newBlock);
                } else if (BukkitUtils.getNonFluidProofBlocks().contains(typeTo)) {
                    consumer.queueBlockReplace(new Actor("WaterFlow"), to.getState(), newBlock);
                } else if (typeTo == Material.LAVA) {
                    int toLevel = ((Levelled)to.getBlockData()).getLevel();
                    if (toLevel == 0) {
                        consumer.queueBlockReplace(new Actor("WaterFlow"), to.getState(), Material.OBSIDIAN.createBlockData());
                    } else if (event.getFace() == BlockFace.DOWN) {
                        consumer.queueBlockReplace(new Actor("WaterFlow"), to.getState(), Material.STONE.createBlockData());
                    }
                }
                if (BukkitUtils.isEmpty(typeTo) || BukkitUtils.getNonFluidProofBlocks().contains(typeTo)) {
                    for (final BlockFace face : new BlockFace[]{BlockFace.DOWN, BlockFace.NORTH, BlockFace.WEST, BlockFace.EAST, BlockFace.SOUTH}) {
                        final Block lower = to.getRelative(face);
                        if (lower.getType() == Material.LAVA) {
                            int toLevel = ((Levelled)lower.getBlockData()).getLevel();
                            if (toLevel == 0) {
                                consumer.queueBlockReplace(new Actor("WaterFlow"), lower.getState(), Material.OBSIDIAN.createBlockData());
                            } else if (event.getFace() == BlockFace.DOWN) {
                                consumer.queueBlockReplace(new Actor("WaterFlow"), lower.getState(), Material.STONE.createBlockData());
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isSurroundedByWater(Block block) {
        for (final BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.WEST, BlockFace.EAST, BlockFace.SOUTH}) {
            if(block.getRelative(face).getType() == Material.WATER) {
                return true;
            }
        }
        return false;
    }
}
