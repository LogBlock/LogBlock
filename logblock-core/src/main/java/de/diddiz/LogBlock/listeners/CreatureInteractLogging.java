package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;
import de.diddiz.LogBlock.util.BukkitUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.TurtleEgg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityInteractEvent;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;

public class CreatureInteractLogging extends LoggingListener {
    public CreatureInteractLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityInteract(EntityInteractEvent event) {
        final WorldConfig wcfg = getWorldConfig(event.getEntity().getWorld());

        // Mobs only
        if (event.getEntity() instanceof Player) {
            return;
        }

        if (wcfg != null) {
            final Block clicked = event.getBlock();
            final Material type = clicked.getType();
            final Location loc = clicked.getLocation();

            if (type == Material.FARMLAND) {
                if (wcfg.isLogging(Logging.CREATURECROPTRAMPLE)) {
                    // 3 = Dirt ID
                    consumer.queueBlock(new Actor("CreatureTrample"), loc, type.createBlockData(), Material.DIRT.createBlockData());
                    // Log the crop on top as being broken
                    Block trampledCrop = clicked.getRelative(BlockFace.UP);
                    if (BukkitUtils.isCropBlock(trampledCrop.getType())) {
                        consumer.queueBlockBreak(new Actor("CreatureTrample"), trampledCrop.getState());
                    }
                }
            } else if (type == Material.TURTLE_EGG) {
                if (wcfg.isLogging(Logging.CREATURECROPTRAMPLE)) {
                    TurtleEgg turtleEggData = (TurtleEgg) clicked.getBlockData();
                    int eggs = turtleEggData.getEggs();
                    if (eggs > 1) {
                        TurtleEgg turtleEggData2 = (TurtleEgg) turtleEggData.clone();
                        turtleEggData2.setEggs(eggs - 1);
                        consumer.queueBlock(new Actor("CreatureTrample"), loc, turtleEggData, turtleEggData2);
                    } else {
                        consumer.queueBlock(new Actor("CreatureTrample"), loc, turtleEggData, Material.AIR.createBlockData());
                    }
                }
            }
        }
    }
}
