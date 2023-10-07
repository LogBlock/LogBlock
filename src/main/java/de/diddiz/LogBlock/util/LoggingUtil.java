package de.diddiz.LogBlock.util;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.Consumer;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Bell;
import org.bukkit.block.data.type.Bell.Attachment;
import org.bukkit.block.data.type.Lantern;
import org.bukkit.block.data.type.PointedDripstone;
import org.bukkit.block.data.type.PointedDripstone.Thickness;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.projectiles.ProjectileSource;
import java.util.List;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;
import static de.diddiz.LogBlock.config.Config.mb4;

public class LoggingUtil {

    public static void smartLogBlockPlace(Consumer consumer, Actor actor, BlockState replaced, BlockState placed) {
        Location loc = replaced.getLocation();
        Material placedType = placed.getType();
        if (!placedType.hasGravity() || !BukkitUtils.canDirectlyFallIn(replaced.getBlock().getRelative(BlockFace.DOWN).getType())) {
            if (placedType == Material.TWISTING_VINES) {
                Block below = placed.getBlock().getRelative(BlockFace.DOWN);
                if (below.getType() == Material.TWISTING_VINES) {
                    consumer.queueBlockReplace(actor, below.getState(), Material.TWISTING_VINES_PLANT.createBlockData());
                }
            }
            if (placedType == Material.WEEPING_VINES) {
                Block above = placed.getBlock().getRelative(BlockFace.UP);
                if (above.getType() == Material.WEEPING_VINES) {
                    consumer.queueBlockReplace(actor, above.getState(), Material.WEEPING_VINES_PLANT.createBlockData());
                }
            }
            if (BukkitUtils.isEmpty(replaced.getType())) {
                consumer.queueBlockPlace(actor, placed);
            } else {
                consumer.queueBlockReplace(actor, replaced, placed);
            }
            return;
        }
        int x = loc.getBlockX();
        int initialy = loc.getBlockY();
        int y = initialy;
        int z = loc.getBlockZ();
        while (y > loc.getWorld().getMinHeight() && BukkitUtils.canFallIn(loc.getWorld(), x, (y - 1), z)) {
            y--;
        }
        if (initialy != y && !BukkitUtils.isEmpty(replaced.getType())) {
            // this is not the final location but the block got removed (vines etc)
            consumer.queueBlockBreak(actor, replaced);
        }
        // If y is minHeight then the block fell out of the world :(
        if (y > loc.getWorld().getMinHeight()) {
            // Run this check to avoid false positives
            Location finalLoc = new Location(loc.getWorld(), x, y, z);
            if (y == initialy || !BukkitUtils.isFallingEntityKiller(finalLoc.getBlock().getType())) {
                if (BukkitUtils.isEmpty(finalLoc.getBlock().getType())) {
                    consumer.queueBlockPlace(actor, finalLoc, placed.getBlockData());
                } else {
                    consumer.queueBlockReplace(actor, finalLoc.getBlock().getState(), placed.getBlockData());
                }
            }
        }
    }

    public static void smartLogFallables(Consumer consumer, Actor actor, Block origin) {

        WorldConfig wcfg = getWorldConfig(origin.getWorld());
        if (wcfg == null) {
            return;
        }

        //Handle falling blocks
        Block checkBlock = origin.getRelative(BlockFace.UP);
        int up = 0;
        final int highestBlock = checkBlock.getWorld().getHighestBlockYAt(checkBlock.getLocation());
        while (checkBlock.getType().hasGravity()) {

            // Record this block as falling
            consumer.queueBlockBreak(actor, checkBlock.getState());

            // Guess where the block is going (This could be thrown of by explosions, but it is better than nothing)
            Location loc = origin.getLocation();
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            while (y > loc.getWorld().getMinHeight() && BukkitUtils.canFallIn(loc.getWorld(), x, (y - 1), z)) {
                y--;
            }
            // If y is minHeight then the sand block fell out of the world :(
            if (y > loc.getWorld().getMinHeight()) {
                Location finalLoc = new Location(loc.getWorld(), x, y, z);
                // Run this check to avoid false positives
                if (!BukkitUtils.isFallingEntityKiller(finalLoc.getBlock().getType())) {
                    finalLoc.add(0, up, 0); // Add this here after checking for block breakers
                    if (BukkitUtils.isEmpty(finalLoc.getBlock().getType())) {
                        consumer.queueBlockPlace(actor, finalLoc, checkBlock.getBlockData());
                    } else {
                        consumer.queueBlockReplace(actor, finalLoc, finalLoc.getBlock().getBlockData(), checkBlock.getBlockData());
                    }
                    up++;
                }
            }
            if (checkBlock.getY() >= highestBlock) {
                break;
            }
            checkBlock = checkBlock.getRelative(BlockFace.UP);
        }
        if (wcfg.isLogging(Logging.SCAFFOLDING) && checkBlock.getType() == Material.SCAFFOLDING && consumer.getLogblock().getScaffoldingLogging() != null) {
            consumer.getLogblock().getScaffoldingLogging().addScaffoldingBreaker(actor, checkBlock);
        }
    }

    public static void smartLogBlockBreak(Consumer consumer, Actor actor, Block origin) {
        smartLogBlockReplace(consumer, actor, origin, null);
    }

    public static void smartLogBlockReplace(Consumer consumer, Actor actor, Block origin, BlockData replacedWith) {

        WorldConfig wcfg = getWorldConfig(origin.getWorld());
        if (wcfg == null) {
            return;
        }
        Material replacedType = origin.getType();
        if (replacedType == Material.TWISTING_VINES || replacedType == Material.TWISTING_VINES_PLANT) {
            Block below = origin.getRelative(BlockFace.DOWN);
            if (below.getType() == Material.TWISTING_VINES_PLANT) {
                consumer.queueBlockReplace(actor, below.getState(), Material.TWISTING_VINES.createBlockData());
            }
        }
        if (replacedType == Material.WEEPING_VINES || replacedType == Material.WEEPING_VINES_PLANT) {
            Block above = origin.getRelative(BlockFace.UP);
            if (above.getType() == Material.WEEPING_VINES_PLANT) {
                consumer.queueBlockReplace(actor, above.getState(), Material.WEEPING_VINES.createBlockData());
            }
        }
        if (replacedType == Material.CAVE_VINES || replacedType == Material.CAVE_VINES_PLANT) {
            Block above = origin.getRelative(BlockFace.UP);
            if (above.getType() == Material.CAVE_VINES_PLANT) {
                consumer.queueBlockReplace(actor, above.getState(), Material.CAVE_VINES.createBlockData());
            }
        }

        Block checkBlock = origin.getRelative(BlockFace.UP);
        Material typeAbove = checkBlock.getType();
        if (BukkitUtils.isRelativeTopBreakable(typeAbove)) {
            if (typeAbove == Material.IRON_DOOR || BukkitUtils.isWoodenDoor(typeAbove)) {
                Block doorBlock = checkBlock;
                // If the doorBlock is the top half a door the player simply punched a door
                // this will be handled later.
                if (!BukkitUtils.isTop(doorBlock.getBlockData())) {
                    doorBlock = doorBlock.getRelative(BlockFace.UP);
                    // Fall back check just in case the top half wasn't a door
                    if (doorBlock.getType() == Material.IRON_DOOR || BukkitUtils.isWoodenDoor(doorBlock.getType())) {
                        consumer.queueBlockBreak(actor, doorBlock.getState());
                    }
                    consumer.queueBlockBreak(actor, checkBlock.getState());
                }
            } else if (BukkitUtils.isDoublePlant(typeAbove)) {
                Block plantBlock = checkBlock;
                // If the plantBlock is the top half of a double plant the player simply
                // punched the plant this will be handled later.
                if (!BukkitUtils.isTop(plantBlock.getBlockData())) {
                    plantBlock = plantBlock.getRelative(BlockFace.UP);
                    // Fall back check just in case the top half wasn't a plant
                    if (BukkitUtils.isDoublePlant(plantBlock.getType())) {
                        consumer.queueBlockBreak(actor, plantBlock.getState());
                    }
                    consumer.queueBlockBreak(actor, checkBlock.getState());
                }
            } else {
                consumer.queueBlockBreak(actor, checkBlock.getState());
                // check next blocks above
                checkBlock = checkBlock.getRelative(BlockFace.UP);
                typeAbove = checkBlock.getType();
                while (BukkitUtils.isRelativeTopBreakable(typeAbove)) {
                    consumer.queueBlockBreak(actor, checkBlock.getState());
                    checkBlock = checkBlock.getRelative(BlockFace.UP);
                    typeAbove = checkBlock.getType();
                }
            }
        } else if (typeAbove == Material.LANTERN) {
            Lantern lantern = (Lantern) checkBlock.getBlockData();
            if (!lantern.isHanging()) {
                consumer.queueBlockBreak(actor, checkBlock.getState());
            }
        } else if (typeAbove == Material.BELL) {
            Bell bell = (Bell) checkBlock.getBlockData();
            if (bell.getAttachment() == Attachment.FLOOR) {
                consumer.queueBlockBreak(actor, checkBlock.getState());
            }
        } else if (typeAbove == Material.POINTED_DRIPSTONE) {
            Block dripStoneBlock = checkBlock;
            while (true) {
                if (dripStoneBlock.getType() != Material.POINTED_DRIPSTONE) {
                    break;
                }
                PointedDripstone dripstone = (PointedDripstone) dripStoneBlock.getBlockData();
                if (dripstone.getVerticalDirection() != BlockFace.UP) {
                    if (dripstone.getThickness() == Thickness.TIP_MERGE) {
                        PointedDripstone newDripstone = (PointedDripstone) dripstone.clone();
                        newDripstone.setThickness(Thickness.TIP);
                        consumer.queueBlockReplace(actor, dripStoneBlock.getState(), newDripstone);
                    }
                    break;
                }
                consumer.queueBlockBreak(actor, dripStoneBlock.getState());
                dripStoneBlock = dripStoneBlock.getRelative(BlockFace.UP);
            }
        }

        checkBlock = origin.getRelative(BlockFace.DOWN);
        Material typeBelow = checkBlock.getType();
        if (typeBelow == Material.LANTERN) {
            Lantern lantern = (Lantern) checkBlock.getBlockData();
            if (lantern.isHanging()) {
                consumer.queueBlockBreak(actor, checkBlock.getState());
            }
        } else if (BukkitUtils.isHangingSign(typeBelow)) {
            consumer.queueBlockBreak(actor, checkBlock.getState());
        } else if (typeBelow == Material.BELL) {
            Bell bell = (Bell) checkBlock.getBlockData();
            if (bell.getAttachment() == Attachment.CEILING) {
                consumer.queueBlockBreak(actor, checkBlock.getState());
            }
        } else if (typeBelow == Material.WEEPING_VINES || typeBelow == Material.WEEPING_VINES_PLANT || typeBelow == Material.CAVE_VINES || typeBelow == Material.CAVE_VINES_PLANT) {
            consumer.queueBlockBreak(actor, checkBlock.getState());
            // check next blocks below
            checkBlock = checkBlock.getRelative(BlockFace.DOWN);
            typeBelow = checkBlock.getType();
            while (typeBelow == Material.WEEPING_VINES || typeBelow == Material.WEEPING_VINES_PLANT || typeBelow == Material.CAVE_VINES || typeBelow == Material.CAVE_VINES_PLANT) {
                consumer.queueBlockBreak(actor, checkBlock.getState());
                checkBlock = checkBlock.getRelative(BlockFace.DOWN);
                typeBelow = checkBlock.getType();
            }
        } else if ((replacedType == Material.BIG_DRIPLEAF || replacedType == Material.BIG_DRIPLEAF_STEM) && (typeBelow == Material.BIG_DRIPLEAF || typeBelow == Material.BIG_DRIPLEAF_STEM)) {
            consumer.queueBlockBreak(actor, checkBlock.getState());
            // check next blocks below
            checkBlock = checkBlock.getRelative(BlockFace.DOWN);
            typeBelow = checkBlock.getType();
            while (typeBelow == Material.BIG_DRIPLEAF || typeBelow == Material.BIG_DRIPLEAF_STEM) {
                consumer.queueBlockBreak(actor, checkBlock.getState());
                checkBlock = checkBlock.getRelative(BlockFace.DOWN);
                typeBelow = checkBlock.getType();
            }
        } else if (typeBelow == Material.POINTED_DRIPSTONE) {
            Block dripStoneBlock = checkBlock;
            while (true) {
                if (dripStoneBlock.getType() != Material.POINTED_DRIPSTONE) {
                    break;
                }
                PointedDripstone dripstone = (PointedDripstone) dripStoneBlock.getBlockData();
                if (dripstone.getVerticalDirection() != BlockFace.DOWN) {
                    if (dripstone.getThickness() == Thickness.TIP_MERGE) {
                        PointedDripstone newDripstone = (PointedDripstone) dripstone.clone();
                        newDripstone.setThickness(Thickness.TIP);
                        consumer.queueBlockReplace(actor, dripStoneBlock.getState(), newDripstone);
                    }
                    break;
                }
                consumer.queueBlockBreak(actor, dripStoneBlock.getState());
                dripStoneBlock = dripStoneBlock.getRelative(BlockFace.DOWN);
            }
        }

        List<Location> relativeBreakables = BukkitUtils.getBlocksNearby(origin, BukkitUtils.getRelativeBreakables());
        if (!relativeBreakables.isEmpty()) {
            for (Location location : relativeBreakables) {
                Block block = location.getBlock();
                BlockData blockData = block.getBlockData();
                if (blockData instanceof Directional) {
                    if (blockData.getMaterial() == Material.BELL) {
                        if (((Bell) blockData).getAttachment() == Attachment.SINGLE_WALL) {
                            if (block.getRelative(((Bell) blockData).getFacing()).equals(origin)) {
                                consumer.queueBlockBreak(actor, block.getState());
                            }
                        }
                    } else {
                        if (block.getRelative(((Directional) blockData).getFacing().getOppositeFace()).equals(origin)) {
                            consumer.queueBlockBreak(actor, block.getState());
                        }
                    }
                }
            }
        }

        // Special door check
        if (replacedType == Material.IRON_DOOR || BukkitUtils.isWoodenDoor(replacedType)) {
            Block doorBlock = origin;

            // Up or down?
            if (!BukkitUtils.isTop(doorBlock.getBlockData())) {
                doorBlock = doorBlock.getRelative(BlockFace.UP);
            } else {
                doorBlock = doorBlock.getRelative(BlockFace.DOWN);
            }

            if (doorBlock.getType() == Material.IRON_DOOR || BukkitUtils.isWoodenDoor(doorBlock.getType())) {
                consumer.queueBlockBreak(actor, doorBlock.getState());
            }
        } else if (BukkitUtils.isDoublePlant(replacedType)) { // Special double plant check
            Block plantBlock = origin;

            // Up or down?
            if (!BukkitUtils.isTop(origin.getBlockData())) {
                plantBlock = plantBlock.getRelative(BlockFace.UP);
            } else {
                plantBlock = plantBlock.getRelative(BlockFace.DOWN);
            }

            if (BukkitUtils.isDoublePlant(plantBlock.getType())) {
                consumer.queueBlockBreak(actor, plantBlock.getState());
            }
        }

        // Do this down here so that the block is added after blocks sitting on it
        if (replacedWith == null) {
            consumer.queueBlockBreak(actor, origin.getState());
        } else {
            consumer.queueBlockReplace(actor, origin.getState(), replacedWith);
        }
    }

    public static String checkText(String text) {
        if (text == null) {
            return text;
        }
        if (mb4) {
            return text;
        }
        return text.replaceAll("[^\\u0000-\\uFFFF]", "?");
    }

    public static Entity getRealDamager(Entity damager) {
        if (damager instanceof Projectile) {
            ProjectileSource realDamager = ((Projectile) damager).getShooter();
            if (realDamager instanceof Entity) {
                damager = (Entity) realDamager;
            }
        }
        if (damager instanceof TNTPrimed) {
            Entity realRemover = ((TNTPrimed) damager).getSource();
            if (realRemover != null) {
                damager = realRemover;
            }
        }
        return damager;
    }
}
