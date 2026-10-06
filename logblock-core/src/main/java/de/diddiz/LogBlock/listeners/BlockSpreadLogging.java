package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.PointedDripstone;
import de.diddiz.LogBlock.platform.DripstoneThickness;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockSpreadEvent;

import static de.diddiz.LogBlock.config.Config.isLogging;

public class BlockSpreadLogging extends LoggingListener {

    public BlockSpreadLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {

        String name;

        World world = event.getNewState().getWorld();
        Material type = event.getNewState().getType();

        if (type == Material.SHORT_GRASS) {
            if (!isLogging(world, Logging.GRASSGROWTH)) {
                return;
            }
            name = "GrassGrowth";
        } else if (type == Material.MYCELIUM) {
            if (!isLogging(world, Logging.MYCELIUMSPREAD)) {
                return;
            }
            name = "MyceliumSpread";
        } else if (type == Material.VINE || type == Material.CAVE_VINES || type == Material.CAVE_VINES_PLANT || type == Material.WEEPING_VINES || type == Material.WEEPING_VINES_PLANT || type == Material.TWISTING_VINES || type == Material.TWISTING_VINES_PLANT) {
            if (!isLogging(world, Logging.VINEGROWTH)) {
                return;
            }
            name = "VineGrowth";
        } else if (type == Material.RED_MUSHROOM || type == Material.BROWN_MUSHROOM) {
            if (!isLogging(world, Logging.MUSHROOMSPREAD)) {
                return;
            }
            name = "MushroomSpread";
        } else if (type == Material.BAMBOO || type == Material.BAMBOO_SAPLING) {
            if (!isLogging(world, Logging.BAMBOOGROWTH)) {
                return;
            }
            name = "BambooGrowth";
            if (type == Material.BAMBOO_SAPLING) {
                // bamboo sapling gets replaced by bamboo
                consumer.queueBlockReplace(new Actor(name), event.getSource().getState(), Material.BAMBOO.createBlockData());
            }
        } else if (type == Material.POINTED_DRIPSTONE) {
            if (!isLogging(world, Logging.DRIPSTONEGROWTH)) {
                return;
            }
            name = "DripstoneGrowth";
            PointedDripstone pointed = (PointedDripstone) event.getNewState().getBlockData();
            if (LogBlock.getInstance().getPlatformAdapter().getDripstoneThickness(pointed) != DripstoneThickness.TIP_MERGE) {
                BlockFace direction = pointed.getVerticalDirection();
                Block previousPart = event.getBlock().getRelative(direction.getOppositeFace());
                if (previousPart.getType() == Material.POINTED_DRIPSTONE) {
                    PointedDripstone newBelow = (PointedDripstone) previousPart.getBlockData();
                    LogBlock.getInstance().getPlatformAdapter().setDripstoneThickness(newBelow, DripstoneThickness.FRUSTUM);
                    consumer.queueBlockReplace(new Actor(name), previousPart.getState(), newBelow);

                    previousPart = previousPart.getRelative(direction.getOppositeFace());
                    if (previousPart.getType() == Material.POINTED_DRIPSTONE) {
                        Block evenMorePrevious = previousPart.getRelative(direction.getOppositeFace());
                        newBelow = (PointedDripstone) previousPart.getBlockData();
                        LogBlock.getInstance().getPlatformAdapter().setDripstoneThickness(newBelow, evenMorePrevious.getType() == Material.POINTED_DRIPSTONE ? DripstoneThickness.MIDDLE : DripstoneThickness.BASE);
                        consumer.queueBlockReplace(new Actor(name), previousPart.getState(), newBelow);
                    }
                }
            } else {
                // special case because the old state is already changed (for one half)
                PointedDripstone oldState = (PointedDripstone) event.getNewState().getBlockData();
                LogBlock.getInstance().getPlatformAdapter().setDripstoneThickness(oldState, DripstoneThickness.TIP);
                consumer.queueBlockReplace(new Actor(name), oldState, event.getNewState());
                return;
            }
        } else if (type == Material.SCULK || type == Material.SCULK_VEIN || type == Material.SCULK_CATALYST || type == Material.SCULK_SENSOR || type == Material.SCULK_SHRIEKER) {
            if (!isLogging(world, Logging.SCULKSPREAD)) {
                return;
            }
            name = "SculkSpread";
        } else {
            return;
        }

        consumer.queueBlockReplace(new Actor(name), event.getBlock().getState(), event.getNewState());
    }
}
