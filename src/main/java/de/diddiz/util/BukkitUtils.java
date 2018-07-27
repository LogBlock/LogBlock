package de.diddiz.util;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class BukkitUtils {
    private static final Set<Set<Integer>> blockEquivalents;
    private static final Set<Material> relativeBreakable;
    private static final Set<Material> relativeTopBreakable;
    private static final Set<Material> fallingEntityKillers;

    private static final Set<Material> cropBlocks;
    private static final Set<Material> containerBlocks;
    
    private static final Set<Material> singleBlockPlants;
    private static final Set<Material> doublePlants;
    
    private static final Set<Material> nonFluidProofBlocks;
    
    private static final Map<EntityType, Material> projectileItems;

    static {
        singleBlockPlants = EnumSet.noneOf(Material.class);
        singleBlockPlants.add(Material.GRASS);
        singleBlockPlants.add(Material.FERN);
        singleBlockPlants.add(Material.DEAD_BUSH);
        singleBlockPlants.add(Material.DANDELION);
        singleBlockPlants.add(Material.POPPY);
        singleBlockPlants.add(Material.BLUE_ORCHID);
        singleBlockPlants.add(Material.ALLIUM);
        singleBlockPlants.add(Material.AZURE_BLUET);
        singleBlockPlants.add(Material.ORANGE_TULIP);
        singleBlockPlants.add(Material.WHITE_TULIP);
        singleBlockPlants.add(Material.PINK_TULIP);
        singleBlockPlants.add(Material.RED_TULIP);
        singleBlockPlants.add(Material.OXEYE_DAISY);        
        singleBlockPlants.add(Material.BROWN_MUSHROOM);
        singleBlockPlants.add(Material.RED_MUSHROOM);
        
        doublePlants = EnumSet.noneOf(Material.class);
        doublePlants.add(Material.TALL_GRASS);
        doublePlants.add(Material.LARGE_FERN);
        doublePlants.add(Material.TALL_SEAGRASS);
        doublePlants.add(Material.ROSE_BUSH);
        doublePlants.add(Material.LILAC);
        doublePlants.add(Material.SUNFLOWER);
        doublePlants.add(Material.PEONY);
        
        blockEquivalents = new HashSet<Set<Integer>>(7);
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(2, 3, 60)));
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(8, 9, 79)));
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(10, 11)));
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(61, 62)));
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(73, 74)));
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(75, 76)));
        blockEquivalents.add(new HashSet<Integer>(Arrays.asList(93, 94)));

        // Blocks that break when they are attached to a block
        relativeBreakable = EnumSet.noneOf(Material.class);
        relativeBreakable.add(Material.WALL_SIGN);
        relativeBreakable.add(Material.LADDER);
        relativeBreakable.add(Material.STONE_BUTTON);
        relativeBreakable.addAll(Tag.WOODEN_BUTTONS.getValues());
        relativeBreakable.add(Material.REDSTONE_WALL_TORCH);
        relativeBreakable.add(Material.LEVER);
        relativeBreakable.add(Material.WALL_TORCH);
        relativeBreakable.add(Material.TRIPWIRE_HOOK);
        relativeBreakable.add(Material.COCOA);

        // Blocks that break when they are on top of a block
        relativeTopBreakable = EnumSet.noneOf(Material.class);
        relativeTopBreakable.addAll(Tag.SAPLINGS.getValues());
        relativeTopBreakable.addAll(singleBlockPlants);
        relativeTopBreakable.add(Material.WHEAT);
        relativeTopBreakable.add(Material.POTATO);
        relativeTopBreakable.add(Material.CARROT);
        relativeTopBreakable.add(Material.LILY_PAD);
        relativeTopBreakable.add(Material.CACTUS);
        relativeTopBreakable.add(Material.SUGAR_CANE);
        relativeTopBreakable.add(Material.FLOWER_POT);
        relativeTopBreakable.add(Material.POWERED_RAIL);
        relativeTopBreakable.add(Material.DETECTOR_RAIL);
        relativeTopBreakable.add(Material.ACTIVATOR_RAIL);
        relativeTopBreakable.add(Material.RAIL);
        relativeTopBreakable.add(Material.REDSTONE_WIRE);
        relativeTopBreakable.add(Material.SIGN);
        relativeTopBreakable.add(Material.STONE_PRESSURE_PLATE);
        relativeTopBreakable.addAll(Tag.WOODEN_PRESSURE_PLATES.getValues());
        relativeTopBreakable.add(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
        relativeTopBreakable.add(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
        relativeTopBreakable.add(Material.SNOW);
        relativeTopBreakable.add(Material.REPEATER);
        relativeTopBreakable.add(Material.COMPARATOR);
        relativeTopBreakable.add(Material.TORCH);
        relativeTopBreakable.add(Material.WALL_TORCH);
        relativeTopBreakable.add(Material.REDSTONE_TORCH);
        relativeTopBreakable.add(Material.REDSTONE_WALL_TORCH);
        relativeTopBreakable.addAll(Tag.WOODEN_DOORS.getValues());
        relativeTopBreakable.add(Material.IRON_DOOR);
        relativeTopBreakable.addAll(Tag.CARPETS.getValues());
        relativeTopBreakable.addAll(doublePlants);

        // Blocks that break falling entities
        fallingEntityKillers = EnumSet.noneOf(Material.class);
        fallingEntityKillers.add(Material.SIGN);
        fallingEntityKillers.add(Material.WALL_SIGN);
        fallingEntityKillers.addAll(Tag.WOODEN_PRESSURE_PLATES.getValues());
        fallingEntityKillers.add(Material.STONE_PRESSURE_PLATE);
        fallingEntityKillers.add(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
        fallingEntityKillers.add(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
        fallingEntityKillers.addAll(Tag.SAPLINGS.getValues());
        fallingEntityKillers.addAll(singleBlockPlants);
        fallingEntityKillers.add(Material.WHEAT);
        fallingEntityKillers.add(Material.CARROT);
        fallingEntityKillers.add(Material.POTATO);
        fallingEntityKillers.add(Material.BEETROOT);
        fallingEntityKillers.add(Material.NETHER_WART);
        fallingEntityKillers.addAll(Tag.SLABS.getValues());
        fallingEntityKillers.add(Material.TORCH);
        fallingEntityKillers.add(Material.WALL_TORCH);
        fallingEntityKillers.add(Material.FLOWER_POT);
        fallingEntityKillers.add(Material.POWERED_RAIL);
        fallingEntityKillers.add(Material.DETECTOR_RAIL);
        fallingEntityKillers.add(Material.ACTIVATOR_RAIL);
        fallingEntityKillers.add(Material.RAIL);
        fallingEntityKillers.add(Material.LEVER);
        fallingEntityKillers.add(Material.REDSTONE_WIRE);
        fallingEntityKillers.add(Material.REDSTONE_TORCH);
        fallingEntityKillers.add(Material.REDSTONE_WALL_TORCH);
        fallingEntityKillers.add(Material.REPEATER);
        fallingEntityKillers.add(Material.COMPARATOR);
        fallingEntityKillers.add(Material.DAYLIGHT_DETECTOR);
        fallingEntityKillers.addAll(Tag.CARPETS.getValues());

        // Crop Blocks
        cropBlocks = EnumSet.noneOf(Material.class);
        cropBlocks.add(Material.WHEAT);
        cropBlocks.add(Material.MELON_STEM);
        cropBlocks.add(Material.PUMPKIN_STEM);
        cropBlocks.add(Material.CARROT);
        cropBlocks.add(Material.POTATO);
        cropBlocks.add(Material.BEETROOT);

        // Container Blocks
        containerBlocks = EnumSet.noneOf(Material.class);
        containerBlocks.add(Material.CHEST);
        containerBlocks.add(Material.TRAPPED_CHEST);
        containerBlocks.add(Material.DISPENSER);
        containerBlocks.add(Material.DROPPER);
        containerBlocks.add(Material.HOPPER);
        containerBlocks.add(Material.BREWING_STAND);
        containerBlocks.add(Material.FURNACE);
        containerBlocks.add(Material.BEACON);
        containerBlocks.add(Material.BLACK_SHULKER_BOX);
        containerBlocks.add(Material.BLUE_SHULKER_BOX);
        containerBlocks.add(Material.LIGHT_GRAY_SHULKER_BOX);
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
        projectileItems = new EnumMap<EntityType, Material>(EntityType.class);
        projectileItems.put(EntityType.ARROW, Material.ARROW);
        projectileItems.put(EntityType.EGG, Material.EGG);
        projectileItems.put(EntityType.ENDER_PEARL, Material.ENDER_PEARL);
        projectileItems.put(EntityType.SMALL_FIREBALL, Material.FIRE_CHARGE);    // Fire charge
        projectileItems.put(EntityType.FIREBALL, Material.FIRE_CHARGE);        // Fire charge
        projectileItems.put(EntityType.FISHING_HOOK, Material.FISHING_ROD);
        projectileItems.put(EntityType.SNOWBALL, Material.SNOWBALL);
        projectileItems.put(EntityType.SPLASH_POTION, Material.SPLASH_POTION);
        projectileItems.put(EntityType.THROWN_EXP_BOTTLE, Material.EXPERIENCE_BOTTLE);
        projectileItems.put(EntityType.WITHER_SKULL, Material.WITHER_SKELETON_SKULL);
        
        nonFluidProofBlocks = EnumSet.noneOf(Material.class);
        nonFluidProofBlocks.addAll(singleBlockPlants);
        nonFluidProofBlocks.addAll(doublePlants);
        nonFluidProofBlocks.add(Material.REDSTONE_WALL_TORCH);
        nonFluidProofBlocks.add(Material.LEVER);
        nonFluidProofBlocks.add(Material.WALL_TORCH);
        nonFluidProofBlocks.add(Material.TRIPWIRE_HOOK);
        nonFluidProofBlocks.add(Material.COCOA);
        nonFluidProofBlocks.addAll(Tag.WOODEN_PRESSURE_PLATES.getValues());
        nonFluidProofBlocks.add(Material.STONE_PRESSURE_PLATE);
        nonFluidProofBlocks.add(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
        nonFluidProofBlocks.add(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
        nonFluidProofBlocks.addAll(Tag.SAPLINGS.getValues());
        nonFluidProofBlocks.add(Material.WHEAT);
        nonFluidProofBlocks.add(Material.CARROT);
        nonFluidProofBlocks.add(Material.POTATO);
        nonFluidProofBlocks.add(Material.BEETROOT);
        nonFluidProofBlocks.add(Material.NETHER_WART);
        nonFluidProofBlocks.add(Material.TORCH);
        nonFluidProofBlocks.add(Material.FLOWER_POT);
        nonFluidProofBlocks.add(Material.POWERED_RAIL);
        nonFluidProofBlocks.add(Material.DETECTOR_RAIL);
        nonFluidProofBlocks.add(Material.ACTIVATOR_RAIL);
        nonFluidProofBlocks.add(Material.RAIL);
        nonFluidProofBlocks.add(Material.LEVER);
        nonFluidProofBlocks.add(Material.REDSTONE_WIRE);
        nonFluidProofBlocks.add(Material.REDSTONE_TORCH);
        nonFluidProofBlocks.add(Material.REPEATER);
        nonFluidProofBlocks.add(Material.COMPARATOR);
        nonFluidProofBlocks.add(Material.DAYLIGHT_DETECTOR);
        nonFluidProofBlocks.addAll(Tag.CARPETS.getValues());

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

    public static boolean isTop(BlockData data) {
        if (data instanceof Bisected && !(data instanceof Stairs)) {
            return ((Bisected) data).getHalf() == Half.TOP;
        }
        return false;
    }

    public static Material getInventoryHolderType(InventoryHolder holder) {
        if (holder instanceof DoubleChest) {
            return getInventoryHolderType(((DoubleChest) holder).getLeftSide());
        } else if (holder instanceof BlockState) {
            return ((BlockState) holder).getType();
        } else {
            return null;
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
        for (int i = 0; i < l1; i++) {
            if (items1[i] != null) {
                items1[i] = new ItemStack(items1[i]);
            }
        }
        for (int i = 0; i < l2; i++) {
            if (items2[i] != null) {
                items2[i] = new ItemStack(items2[i]);
            }
        }
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
                boolean found = false;
                for (final ItemStack item2 : compressed) {
                    if (item2.isSimilar(item)) {
                        item2.setAmount(item2.getAmount() + item.getAmount());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    compressed.add(item.clone());
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

    public static Set<Material> getFallingEntityKillers() {
        return fallingEntityKillers;
    }
    
    public static Set<Material> getNonFluidProofBlocks() {
        return nonFluidProofBlocks;
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

    public static void giveTool(Player player, Material type) {
        final Inventory inv = player.getInventory();
        if (inv.contains(type)) {
            player.sendMessage(ChatColor.RED + "You have already a " + type.name());
        } else {
            final int free = inv.firstEmpty();
            if (free >= 0) {
                if (player.getInventory().getItemInMainHand() != null && player.getInventory().getItemInMainHand().getType() != Material.AIR) {
                    inv.setItem(free, player.getInventory().getItemInMainHand());
                }
                player.getInventory().setItemInMainHand(new ItemStack(type));
                player.sendMessage(ChatColor.GREEN + "Here's your " + type.name());
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
        boolean lower = world.getBlockAt(x, y, z).isEmpty(), upper = world.getBlockAt(x, y + 1, z).isEmpty();
        while ((!lower || !upper) && y != 127) {
            lower = upper;
            upper = world.getBlockAt(x, ++y, z).isEmpty();
        }
        while (world.getBlockAt(x, y - 1, z).isEmpty() && y != 0) {
            y--;
        }
        return y;
    }

    public static int modifyContainer(BlockState b, ItemStack item, boolean remove) {
        if (b instanceof InventoryHolder) {
            final Inventory inv = ((InventoryHolder) b).getInventory();
            if (remove) {
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
        } else if (mat == Material.WATER || mat == Material.LAVA) { // Fluids
            return true;
        } else if (getFallingEntityKillers().contains(mat) || mat == Material.FIRE || mat == Material.VINE || doublePlants.contains(mat) || mat == Material.DEAD_BUSH) { // Misc.
            return true;
        }
        return false;
    }

    public static class ItemStackComparator implements Comparator<ItemStack> {
        @Override
        public int compare(ItemStack a, ItemStack b) {
            return a.getType().name().compareTo(b.getType().name());
        }
    }

    public static Material itemIDfromProjectileEntity(Entity e) {
        return projectileItems.get(e.getType());
    }
    
    public static boolean isDoublePlant(Material m) {
        return doublePlants.contains(m);
    }

    public static boolean isEmpty(Material m) {
        return m == Material.AIR || m == Material.CAVE_AIR || m == Material.VOID_AIR;
    }
}
