package de.diddiz.util;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

import static de.diddiz.util.MaterialName.materialName;

public class BukkitUtils {
    private static final Set<Set<Integer>> blockEquivalents;
    private static final Set<Material> relativeBreakable;
    private static final Set<Material> relativeTopBreakable;
    private static final Set<Material> relativeTopFallables;
    private static final Set<Material> fallingEntityKillers;

    private static final Set<Material> cropBlocks;
    private static final Set<Material> containerBlocks;

    private static final Map<EntityType, Integer> projectileItems;

    static {
        blockEquivalents = new HashSet<Set<Integer>>(7);
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(2, 3, 60)));
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(8, 9, 79)));
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(10, 11)));
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(61, 62)));
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(73, 74)));
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(75, 76)));
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(93, 94)));

        // Blocks that break when they are attached to a block
        relativeBreakable = new HashSet<Material>(11);
        relativeBreakable.add(Material.WALL_SIGN);
        relativeBreakable.add(Material.LADDER);
        relativeBreakable.add(Material.STONE_BUTTON);
        relativeBreakable.add(Material.WOOD_BUTTON);
        relativeBreakable.add(Material.REDSTONE_TORCH_ON);
        relativeBreakable.add(Material.REDSTONE_TORCH_OFF);
        relativeBreakable.add(Material.LEVER);
        relativeBreakable.add(Material.TORCH);
        relativeBreakable.add(Material.TRAP_DOOR);
        relativeBreakable.add(Material.TRIPWIRE_HOOK);
        relativeBreakable.add(Material.COCOA);

        // Blocks that break when they are on top of a block
        relativeTopBreakable = new HashSet<Material>(33);
        relativeTopBreakable.add(Material.SAPLING);
        relativeTopBreakable.add(Material.LONG_GRASS);
        relativeTopBreakable.add(Material.DEAD_BUSH);
        relativeTopBreakable.add(Material.YELLOW_FLOWER);
        relativeTopBreakable.add(Material.RED_ROSE);
        relativeTopBreakable.add(Material.BROWN_MUSHROOM);
        relativeTopBreakable.add(Material.RED_MUSHROOM);
        relativeTopBreakable.add(Material.CROPS);
        relativeTopBreakable.add(Material.POTATO);
        relativeTopBreakable.add(Material.CARROT);
        relativeTopBreakable.add(Material.WATER_LILY);
        relativeTopBreakable.add(Material.CACTUS);
        relativeTopBreakable.add(Material.SUGAR_CANE_BLOCK);
        relativeTopBreakable.add(Material.FLOWER_POT);
        relativeTopBreakable.add(Material.POWERED_RAIL);
        relativeTopBreakable.add(Material.DETECTOR_RAIL);
        relativeTopBreakable.add(Material.ACTIVATOR_RAIL);
        relativeTopBreakable.add(Material.RAILS);
        relativeTopBreakable.add(Material.REDSTONE_WIRE);
        relativeTopBreakable.add(Material.SIGN_POST);
        relativeTopBreakable.add(Material.STONE_PLATE);
        relativeTopBreakable.add(Material.WOOD_PLATE);
        relativeTopBreakable.add(Material.IRON_PLATE);
        relativeTopBreakable.add(Material.GOLD_PLATE);
        relativeTopBreakable.add(Material.SNOW);
        relativeTopBreakable.add(Material.DIODE_BLOCK_ON);
        relativeTopBreakable.add(Material.DIODE_BLOCK_OFF);
        relativeTopBreakable.add(Material.REDSTONE_COMPARATOR_ON);
        relativeTopBreakable.add(Material.REDSTONE_COMPARATOR_OFF);
        relativeTopBreakable.add(Material.WOODEN_DOOR);
        relativeTopBreakable.add(Material.IRON_DOOR_BLOCK);
        relativeTopBreakable.add(Material.CARPET);
        relativeTopBreakable.add(Material.DOUBLE_PLANT);

        // Blocks that fall
        relativeTopFallables = new HashSet<Material>(4);
        relativeTopFallables.add(Material.SAND);
        relativeTopFallables.add(Material.GRAVEL);
        relativeTopFallables.add(Material.DRAGON_EGG);
        relativeTopFallables.add(Material.ANVIL);
        relativeTopFallables.add(Material.CONCRETE_POWDER);

        // Blocks that break falling entities
        fallingEntityKillers = new HashSet<Material>(32);
        fallingEntityKillers.add(Material.SIGN_POST);
        fallingEntityKillers.add(Material.WALL_SIGN);
        fallingEntityKillers.add(Material.STONE_PLATE);
        fallingEntityKillers.add(Material.WOOD_PLATE);
        fallingEntityKillers.add(Material.IRON_PLATE);
        fallingEntityKillers.add(Material.GOLD_PLATE);
        fallingEntityKillers.add(Material.SAPLING);
        fallingEntityKillers.add(Material.YELLOW_FLOWER);
        fallingEntityKillers.add(Material.RED_ROSE);
        fallingEntityKillers.add(Material.CROPS);
        fallingEntityKillers.add(Material.CARROT);
        fallingEntityKillers.add(Material.POTATO);
        fallingEntityKillers.add(Material.RED_MUSHROOM);
        fallingEntityKillers.add(Material.BROWN_MUSHROOM);
        fallingEntityKillers.add(Material.STEP);
        fallingEntityKillers.add(Material.WOOD_STEP);
        fallingEntityKillers.add(Material.TORCH);
        fallingEntityKillers.add(Material.FLOWER_POT);
        fallingEntityKillers.add(Material.POWERED_RAIL);
        fallingEntityKillers.add(Material.DETECTOR_RAIL);
        fallingEntityKillers.add(Material.ACTIVATOR_RAIL);
        fallingEntityKillers.add(Material.RAILS);
        fallingEntityKillers.add(Material.LEVER);
        fallingEntityKillers.add(Material.REDSTONE_WIRE);
        fallingEntityKillers.add(Material.REDSTONE_TORCH_ON);
        fallingEntityKillers.add(Material.REDSTONE_TORCH_OFF);
        fallingEntityKillers.add(Material.DIODE_BLOCK_ON);
        fallingEntityKillers.add(Material.DIODE_BLOCK_OFF);
        fallingEntityKillers.add(Material.REDSTONE_COMPARATOR_ON);
        fallingEntityKillers.add(Material.REDSTONE_COMPARATOR_OFF);
        fallingEntityKillers.add(Material.DAYLIGHT_DETECTOR);
        fallingEntityKillers.add(Material.CARPET);

        // Crop Blocks
        cropBlocks = new HashSet<Material>(5);
        cropBlocks.add(Material.CROPS);
        cropBlocks.add(Material.MELON_STEM);
        cropBlocks.add(Material.PUMPKIN_STEM);
        cropBlocks.add(Material.CARROT);
        cropBlocks.add(Material.POTATO);

        // Container Blocks
        containerBlocks = new HashSet<Material>(6);
        containerBlocks.add(Material.CHEST);
        containerBlocks.add(Material.TRAPPED_CHEST);
        containerBlocks.add(Material.DISPENSER);
        containerBlocks.add(Material.DROPPER);
        containerBlocks.add(Material.HOPPER);
        containerBlocks.add(Material.BREWING_STAND);
        containerBlocks.add(Material.FURNACE);
        containerBlocks.add(Material.BURNING_FURNACE);
        containerBlocks.add(Material.BEACON);
        containerBlocks.add(Material.BLACK_SHULKER_BOX);
        containerBlocks.add(Material.BLUE_SHULKER_BOX);
        containerBlocks.add(Material.SILVER_SHULKER_BOX);
        containerBlocks.add(Material.BROWN_SHULKER_BOX);
        containerBlocks.add(Material.CYAN_SHULKER_BOX);
        containerBlocks.add(Material.GRAY_SHULKER_BOX);
        containerBlocks.add(Material.GREEN_SHULKER_BOX);
        containerBlocks.add(Material.LIGHT_BLUE_SHULKER_BOX);
        containerBlocks.add(Material.MAGENTA_SHULKER_BOX);
        containerBlocks.add(Material.LIME_SHULKER_BOX);
        containerBlocks.add(Material.ORANGE_SHULKER_BOX);
        containerBlocks.add(Material.PINK_SHULKER_BOX);
        containerBlocks.add(Material.PURPLE_SHULKER_BOX);
        containerBlocks.add(Material.RED_SHULKER_BOX);
        containerBlocks.add(Material.WHITE_SHULKER_BOX);
        containerBlocks.add(Material.YELLOW_SHULKER_BOX);
        // Doesn't actually have a block inventory
        // containerBlocks.add(Material.ENDER_CHEST);

        // It doesn't seem like you could injure people with some of these, but they exist, so....
        projectileItems = new EnumMap<EntityType, Integer>(EntityType.class);
        projectileItems.put(EntityType.ARROW, 262);
        projectileItems.put(EntityType.EGG, 344);
        projectileItems.put(EntityType.ENDER_PEARL, 368);
        projectileItems.put(EntityType.SMALL_FIREBALL, 385);    // Fire charge
        projectileItems.put(EntityType.FIREBALL, 385);        // Fire charge
        projectileItems.put(EntityType.FISHING_HOOK, 346);
        projectileItems.put(EntityType.SNOWBALL, 332);
        projectileItems.put(EntityType.SPLASH_POTION, 373);
        projectileItems.put(EntityType.THROWN_EXP_BOTTLE, 384);
        projectileItems.put(EntityType.WITHER_SKULL, 397);

    }

    private static final BlockFace[] relativeBlockFaces = new BlockFace[]{
            BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP, BlockFace.DOWN
    };

    /**
     * Returns a list of block locations around the block that are of the type specified by the integer list parameter
     *
     * @param block The central block to get the blocks around
     * @param type The type of blocks around the center block to return
     * @return List of block locations around the block that are of the type specified by the integer list parameter
     */
    public static List<Location> getBlocksNearby(org.bukkit.block.Block block, Set<Material> type) {
        ArrayList<Location> blocks = new ArrayList<Location>();
        for (BlockFace blockFace : relativeBlockFaces) {
            if (type.contains(block.getRelative(blockFace).getType())) {
                blocks.add(block.getRelative(blockFace).getLocation());
            }
        }
        return blocks;
    }

    public static boolean isTop(Material mat, byte data) {

        switch (mat) {
            case DOUBLE_PLANT:
                return data > 5;
            case IRON_DOOR_BLOCK:
            case WOODEN_DOOR:
                return data == 8 || data == 9;
            default:
                return false;
        }
    }

    public static int getInventoryHolderType(InventoryHolder holder) {
        if (holder instanceof DoubleChest) {
            return getInventoryHolderType(((DoubleChest) holder).getLeftSide());
        } else if (holder instanceof BlockState) {
            return ((BlockState) holder).getTypeId();
        } else {
            return -1;
        }
    }

    public static Location getInventoryHolderLocation(InventoryHolder holder) {
        if (holder instanceof DoubleChest) {
            return getInventoryHolderLocation(((DoubleChest) holder).getLeftSide());
        } else if (holder instanceof BlockState) {
            return ((BlockState) holder).getLocation();
        } else {
            return null;
        }
    }

    public static ItemStack[] compareInventories(ItemStack[] items1, ItemStack[] items2) {
        final ItemStackComparator comperator = new ItemStackComparator();
        final ArrayList<ItemStack> diff = new ArrayList<ItemStack>();
        final int l1 = items1.length, l2 = items2.length;
        int c1 = 0, c2 = 0;
        while (c1 < l1 || c2 < l2) {
            if (c1 >= l1) {
                diff.add(items2[c2]);
                c2++;
                continue;
            }
            if (c2 >= l2) {
                items1[c1].setAmount(items1[c1].getAmount() * -1);
                diff.add(items1[c1]);
                c1++;
                continue;
            }
            final int comp = comperator.compare(items1[c1], items2[c2]);
            if (comp < 0) {
                items1[c1].setAmount(items1[c1].getAmount() * -1);
                diff.add(items1[c1]);
                c1++;
            } else if (comp > 0) {
                diff.add(items2[c2]);
                c2++;
            } else {
                final int amount = items2[c2].getAmount() - items1[c1].getAmount();
                if (amount != 0) {
                    items1[c1].setAmount(amount);
                    diff.add(items1[c1]);
                }
                c1++;
                c2++;
            }
        }
        return diff.toArray(new ItemStack[diff.size()]);
    }

    public static ItemStack[] compressInventory(ItemStack[] items) {
        final ArrayList<ItemStack> compressed = new ArrayList<ItemStack>();
        for (final ItemStack item : items) {
            if (item != null) {
                final int type = item.getTypeId();
                final short data = rawData(item);
                boolean found = false;
                for (final ItemStack item2 : compressed) {
                    if (type == item2.getTypeId() && data == rawData(item2)) {
                        item2.setAmount(item2.getAmount() + item.getAmount());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    compressed.add(new ItemStack(type, item.getAmount(), data));
                }
            }
        }
        Collections.sort(compressed, new ItemStackComparator());
        return compressed.toArray(new ItemStack[compressed.size()]);
    }

    public static boolean equalTypes(int type1, int type2) {
        if (type1 == type2) {
            return true;
        }
        for (final Set<Integer> equivalent : blockEquivalents) {
            if (equivalent.contains(type1) && equivalent.contains(type2)) {
                return true;
            }
        }
        return false;
    }

    public static String friendlyWorldname(String worldName) {
        return new File(worldName).getName();
    }

    public static Set<Set<Integer>> getBlockEquivalents() {
        return blockEquivalents;
    }

    public static Set<Material> getRelativeBreakables() {
        return relativeBreakable;
    }

    public static Set<Material> getRelativeTopBreakabls() {
        return relativeTopBreakable;
    }

    public static Set<Material> getRelativeTopFallables() {
        return relativeTopFallables;
    }

    public static Set<Material> getFallingEntityKillers() {
        return fallingEntityKillers;
    }

    public static Set<Material> getCropBlocks() {
        return cropBlocks;
    }

    public static Set<Material> getContainerBlocks() {
        return containerBlocks;
    }

    public static String entityName(Entity entity) {
        if (entity instanceof Player) {
            return ((Player) entity).getName();
        }
        if (entity instanceof TNTPrimed) {
            return "TNT";
        }
        return entity.getClass().getSimpleName().substring(5);
    }

    public static void giveTool(Player player, int type) {
        final Inventory inv = player.getInventory();
        if (inv.contains(type)) {
            player.sendMessage(ChatColor.RED + "You have already a " + materialName(type));
        } else {
            final int free = inv.firstEmpty();
            if (free >= 0) {
                if (player.getItemInHand() != null && player.getItemInHand().getTypeId() != 0) {
                    inv.setItem(free, player.getItemInHand());
                }
                player.setItemInHand(new ItemStack(type, 1));
                player.sendMessage(ChatColor.GREEN + "Here's your " + materialName(type));
            } else {
                player.sendMessage(ChatColor.RED + "You have no empty slot in your inventory");
            }
        }
    }

    public static short rawData(ItemStack item) {
        return item.getType() != null ? item.getData() != null ? item.getDurability() : 0 : 0;
    }

    public static int saveSpawnHeight(Location loc) {
        final World world = loc.getWorld();
        final Chunk chunk = world.getChunkAt(loc);
        if (!world.isChunkLoaded(chunk)) {
            world.loadChunk(chunk);
        }
        final int x = loc.getBlockX(), z = loc.getBlockZ();
        int y = loc.getBlockY();
        boolean lower = world.getBlockTypeIdAt(x, y, z) == 0, upper = world.getBlockTypeIdAt(x, y + 1, z) == 0;
        while ((!lower || !upper) && y != 127) {
            lower = upper;
            upper = world.getBlockTypeIdAt(x, ++y, z) == 0;
        }
        while (world.getBlockTypeIdAt(x, y - 1, z) == 0 && y != 0) {
            y--;
        }
        return y;
    }

    public static int modifyContainer(BlockState b, ItemStack item) {
        if (b instanceof InventoryHolder) {
            final Inventory inv = ((InventoryHolder) b).getInventory();
            if (item.getAmount() < 0) {
                item.setAmount(-item.getAmount());
                final ItemStack tmp = inv.removeItem(item).get(0);
                return tmp != null ? tmp.getAmount() : 0;
            } else if (item.getAmount() > 0) {
                final ItemStack tmp = inv.addItem(item).get(0);
                return tmp != null ? tmp.getAmount() : 0;
            }
        }
        return 0;
    }

    public static boolean canFall(World world, int x, int y, int z) {
        Material mat = world.getBlockAt(x, y, z).getType();

        // Air
        if (mat == Material.AIR) {
            return true;
        } else if (mat == Material.WATER || mat == Material.STATIONARY_WATER || mat == Material.LAVA || mat == Material.STATIONARY_LAVA) { // Fluids
            return true;
        } else if (getFallingEntityKillers().contains(mat) || mat == Material.FIRE || mat == Material.VINE || mat == Material.LONG_GRASS || mat == Material.DEAD_BUSH) { // Misc.
            return true;
        }
        return false;
    }

    public static class ItemStackComparator implements Comparator<ItemStack> {
        @Override
        public int compare(ItemStack a, ItemStack b) {
            final int aType = a.getTypeId(), bType = b.getTypeId();
            if (aType < bType) {
                return -1;
            }
            if (aType > bType) {
                return 1;
            }
            final short aData = rawData(a), bData = rawData(b);
            if (aData < bData) {
                return -1;
            }
            if (aData > bData) {
                return 1;
            }
            return 0;
        }
    }

    public static int itemIDfromProjectileEntity(Entity e) {
        Integer i = projectileItems.get(e.getType());
        return (i == null) ? 0 : i;
    }
}
