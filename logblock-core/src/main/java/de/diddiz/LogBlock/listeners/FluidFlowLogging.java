package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.Config;
import de.diddiz.LogBlock.config.WorldConfig;
import de.diddiz.LogBlock.util.BukkitUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;

public class FluidFlowLogging extends LoggingListener {

    public FluidFlowLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        final WorldConfig wcfg = getWorldConfig(event.getBlock().getWorld());
        if (wcfg != null && (wcfg.isLogging(Logging.WATERFLOW) || wcfg.isLogging(Logging.LAVAFLOW))) {
            final BlockData blockDataFrom = event.getBlock().getBlockData();
            Material typeFrom = blockDataFrom.getMaterial();
            boolean fromWaterlogged = false;
            if (blockDataFrom instanceof Waterlogged) {
                typeFrom = Material.WATER;
                fromWaterlogged = true;
            }
            if (typeFrom == Material.SEAGRASS || typeFrom == Material.KELP_PLANT || typeFrom == Material.KELP) {
                typeFrom = Material.WATER;
                fromWaterlogged = true;
            }

            Block source = Config.logFluidFlowAsPlayerWhoTriggeredIt ? event.getBlock() : null;
            final Block to = event.getToBlock();
            final Material typeTo = to.getType();
            boolean down = event.getFace() == BlockFace.DOWN;
            final boolean canFlow = BukkitUtils.isEmpty(typeTo) || BukkitUtils.isNonFluidProofBlock(typeTo);
            if (typeFrom == Material.LAVA && wcfg.isLogging(Logging.LAVAFLOW)) {
                Levelled levelledFrom = (Levelled) blockDataFrom;
                if (canFlow) {
                    if (isSurroundedByWater(to) && levelledFrom.getLevel() <= 2) {
                        consumer.queueBlockReplace(new Actor("LavaFlow", source), to.getState(), Material.COBBLESTONE.createBlockData());
                    } else {
                        Levelled newBlock = (Levelled) blockDataFrom.clone();
                        newBlock.setLevel(down ? 1 : Math.min(levelledFrom.getLevel() + 1, levelledFrom.getMaximumLevel()));
                        if (BukkitUtils.isEmpty(typeTo)) {
                            consumer.queueBlockPlace(new Actor("LavaFlow", source), to.getLocation(), newBlock);
                        } else {
                            consumer.queueBlockReplace(new Actor("LavaFlow", source), to.getState(), newBlock);
                        }
                    }
                } else if (typeTo == Material.WATER) {
                    if (down) {
                        consumer.queueBlockReplace(new Actor("LavaFlow", source), to.getState(), Material.STONE.createBlockData());
                    } else {
                        consumer.queueBlockReplace(new Actor("LavaFlow", source), to.getState(), Material.COBBLESTONE.createBlockData());
                    }
                }
            } else if ((typeFrom == Material.WATER) && wcfg.isLogging(Logging.WATERFLOW)) {
                Levelled levelledFrom = fromWaterlogged ? null : (Levelled) blockDataFrom;
                Levelled newBlock = (Levelled) Material.WATER.createBlockData();
                newBlock.setLevel(fromWaterlogged || down ? 1 : Math.min(levelledFrom.getLevel() + 1, levelledFrom.getMaximumLevel()));
                if (BukkitUtils.isEmpty(typeTo)) {
                    consumer.queueBlockPlace(new Actor("WaterFlow", source), to.getLocation(), newBlock);
                } else if (BukkitUtils.isNonFluidProofBlock(typeTo)) {
                    consumer.queueBlockReplace(new Actor("WaterFlow", source), to.getState(), newBlock);
                } else if (typeTo == Material.LAVA) {
                    int toLevel = ((Levelled) to.getBlockData()).getLevel();
                    if (toLevel == 0) {
                        consumer.queueBlockReplace(new Actor("WaterFlow", source), to.getState(), Material.OBSIDIAN.createBlockData());
                    } else if (event.getFace() == BlockFace.DOWN) {
                        consumer.queueBlockReplace(new Actor("WaterFlow", source), to.getState(), Material.STONE.createBlockData());
                    }
                }
                if (BukkitUtils.isEmpty(typeTo) || BukkitUtils.isNonFluidProofBlock(typeTo)) {
                    for (final BlockFace face : new BlockFace[] { BlockFace.DOWN, BlockFace.NORTH, BlockFace.WEST, BlockFace.EAST, BlockFace.SOUTH }) {
                        final Block lower = to.getRelative(face);
                        if (lower.getType() == Material.LAVA) {
                            int toLevel = ((Levelled) lower.getBlockData()).getLevel();
                            if (toLevel == 0) {
                                consumer.queueBlockReplace(new Actor("WaterFlow", source), lower.getState(), Material.OBSIDIAN.createBlockData());
                            } else if (event.getFace() == BlockFace.DOWN) {
                                consumer.queueBlockReplace(new Actor("WaterFlow", source), lower.getState(), Material.STONE.createBlockData());
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        final WorldConfig wcfg = getWorldConfig(event.getBlock().getWorld());
        if (wcfg != null && (wcfg.isLogging(Logging.WATERFLOW) || wcfg.isLogging(Logging.LAVAFLOW))) {
            if (wcfg.isLogging(Logging.LAVAFLOW) && event.getBlock().getType() == Material.WATER && event.getNewState().getType() == Material.COBBLESTONE) {
                consumer.queueBlockReplace(new Actor("LavaFlow"), event.getBlock().getBlockData(), event.getNewState());
            }
            if (wcfg.isLogging(Logging.WATERFLOW) && event.getBlock().getType() == Material.LAVA) {
                consumer.queueBlockReplace(new Actor("WaterFlow"), event.getBlock().getBlockData(), event.getNewState());
            }
            if (wcfg.isLogging(Logging.WATERFLOW) && BukkitUtils.isConcreteBlock(event.getNewState().getType())) {
                consumer.queueBlockReplace(new Actor("WaterFlow"), event.getBlock().getBlockData(), event.getNewState());
            }
        }
    }

    private static boolean isSurroundedByWater(Block block) {
        for (final BlockFace face : new BlockFace[] { BlockFace.NORTH, BlockFace.WEST, BlockFace.EAST, BlockFace.SOUTH }) {
            if (block.getRelative(face).getType() == Material.WATER) {
                return true;
            }
        }
        return false;
    }
}
