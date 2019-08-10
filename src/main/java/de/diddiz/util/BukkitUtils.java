package de.diddiz.util;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Slab.Type;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

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

    private static final Set<Material> bedBlocks;

    private static final Map<EntityType, Material> projectileItems;
    private static final EnumSet<Material> buttons;
    private static final EnumSet<Material> pressurePlates;
    private static final EnumSet<Material> woodenDoors;
    private static final EnumSet<Material> slabs;
    private static final EnumSet<Material> concreteBlocks;
    private static final EnumMap<Material, DyeColor> dyes;

    static {
        pressurePlates = EnumSet.noneOf(Material.class);
        pressurePlates.add(Material.OAK_PRESSURE_PLATE);
        pressurePlates.add(Material.SPRUCE_PRESSURE_PLATE);
        pressurePlates.add(Material.BIRCH_PRESSURE_PLATE);
        pressurePlates.add(Material.JUNGLE_PRESSURE_PLATE);
        pressurePlates.add(Material.ACACIA_PRESSURE_PLATE);
        pressurePlates.add(Material.DARK_OAK_PRESSURE_PLATE);
        pressurePlates.add(Material.STONE_PRESSURE_PLATE);
        pressurePlates.add(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
        pressurePlates.add(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);

        woodenDoors = EnumSet.noneOf(Material.class);
        woodenDoors.add(Material.OAK_DOOR);
        woodenDoors.add(Material.SPRUCE_DOOR);
        woodenDoors.add(Material.BIRCH_DOOR);
        woodenDoors.add(Material.JUNGLE_DOOR);
        woodenDoors.add(Material.ACACIA_DOOR);
        woodenDoors.add(Material.DARK_OAK_DOOR);

        EnumSet<Material> saplings = EnumSet.noneOf(Material.class);
        saplings.add(Material.OAK_SAPLING);
        saplings.add(Material.SPRUCE_SAPLING);
        saplings.add(Material.BIRCH_SAPLING);
        saplings.add(Material.JUNGLE_SAPLING);
        saplings.add(Material.ACACIA_SAPLING);
        saplings.add(Material.DARK_OAK_SAPLING);

        EnumSet<Material> carpets = EnumSet.noneOf(Material.class);
        carpets.add(Material.BLACK_CARPET);
        carpets.add(Material.BLUE_CARPET);
        carpets.add(Material.LIGHT_GRAY_CARPET);
        carpets.add(Material.BROWN_CARPET);
        carpets.add(Material.CYAN_CARPET);
        carpets.add(Material.GRAY_CARPET);
        carpets.add(Material.GREEN_CARPET);
        carpets.add(Material.LIGHT_BLUE_CARPET);
        carpets.add(Material.MAGENTA_CARPET);
        carpets.add(Material.LIME_CARPET);
        carpets.add(Material.ORANGE_CARPET);
        carpets.add(Material.PINK_CARPET);
        carpets.add(Material.PURPLE_CARPET);
        carpets.add(Material.RED_CARPET);
        carpets.add(Material.WHITE_CARPET);
        carpets.add(Material.YELLOW_CARPET);

        slabs = EnumSet.noneOf(Material.class);
        slabs.add(Material.OAK_SLAB);
        slabs.add(Material.SPRUCE_SLAB);
        slabs.add(Material.BIRCH_SLAB);
        slabs.add(Material.JUNGLE_SLAB);
        slabs.add(Material.ACACIA_SLAB);
        slabs.add(Material.DARK_OAK_SLAB);
        slabs.add(Material.STONE_SLAB);
        slabs.add(Material.STONE_BRICK_SLAB);
        slabs.add(Material.COBBLESTONE_SLAB);
        slabs.add(Material.PETRIFIED_OAK_SLAB);
        slabs.add(Material.SANDSTONE_SLAB);
        slabs.add(Material.RED_SANDSTONE_SLAB);
        slabs.add(Material.NETHER_BRICK_SLAB);
        slabs.add(Material.PURPUR_SLAB);
        slabs.add(Material.QUARTZ_SLAB);
        slabs.add(Material.BRICK_SLAB);
        slabs.add(Material.PRISMARINE_SLAB);
        slabs.add(Material.DARK_PRISMARINE_SLAB);
        slabs.add(Material.PRISMARINE_BRICK_SLAB);

        buttons = EnumSet.noneOf(Material.class);
        buttons.add(Material.STONE_BUTTON);
        buttons.add(Material.OAK_BUTTON);
        buttons.add(Material.SPRUCE_BUTTON);
        buttons.add(Material.BIRCH_BUTTON);
        buttons.add(Material.JUNGLE_BUTTON);
        buttons.add(Material.ACACIA_BUTTON);
        buttons.add(Material.DARK_OAK_BUTTON);

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
        singleBlockPlants.add(Material.SWEET_BERRY_BUSH);
        singleBlockPlants.add(Material.LILY_OF_THE_VALLEY);
        singleBlockPlants.add(Material.CORNFLOWER);
        singleBlockPlants.add(Material.WITHER_ROSE);

        doublePlants = EnumSet.noneOf(Material.class);
        doublePlants.add(Material.TALL_GRASS);
        doublePlants.add(Material.LARGE_FERN);
        doublePlants.add(Material.TALL_SEAGRASS);
        doublePlants.add(Material.ROSE_BUSH);
        doublePlants.add(Material.LILAC);
        doublePlants.add(Material.SUNFLOWER);
        doublePlants.add(Material.PEONY);

        blockEquivalents = new HashSet<>(7);
        blockEquivalents.add(new HashSet<>(Arrays.asList(2, 3, 60)));
        blockEquivalents.add(new HashSet<>(Arrays.asList(8, 9, 79)));
        blockEquivalents.add(new HashSet<>(Arrays.asList(10, 11)));
        blockEquivalents.add(new HashSet<>(Arrays.asList(61, 62)));
        blockEquivalents.add(new HashSet<>(Arrays.asList(73, 74)));
        blockEquivalents.add(new HashSet<>(Arrays.asList(75, 76)));
        blockEquivalents.add(new HashSet<>(Arrays.asList(93, 94)));

        // Blocks that break when they are attached to a block
        relativeBreakable = EnumSet.noneOf(Material.class);
        relativeBreakable.add(Material.ACACIA_WALL_SIGN);
        relativeBreakable.add(Material.BIRCH_WALL_SIGN);
        relativeBreakable.add(Material.DARK_OAK_WALL_SIGN);
        relativeBreakable.add(Material.JUNGLE_WALL_SIGN);
        relativeBreakable.add(Material.OAK_WALL_SIGN);
        relativeBreakable.add(Material.SPRUCE_WALL_SIGN);
        relativeBreakable.add(Material.LADDER);
        relativeBreakable.addAll(buttons);
        relativeBreakable.add(Material.REDSTONE_WALL_TORCH);
        relativeBreakable.add(Material.LEVER);
        relativeBreakable.add(Material.WALL_TORCH);
        relativeBreakable.add(Material.TRIPWIRE_HOOK);
        relativeBreakable.add(Material.COCOA);
        relativeBreakable.add(Material.BELL);

        // Blocks that break when they are on top of a block
        relativeTopBreakable = EnumSet.noneOf(Material.class);
        relativeTopBreakable.addAll(saplings);
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
        relativeTopBreakable.add(Material.ACACIA_SIGN);
        relativeTopBreakable.add(Material.BIRCH_SIGN);
        relativeTopBreakable.add(Material.DARK_OAK_SIGN);
        relativeTopBreakable.add(Material.JUNGLE_SIGN);
        relativeTopBreakable.add(Material.OAK_SIGN);
        relativeTopBreakable.add(Material.SPRUCE_SIGN);
        relativeTopBreakable.addAll(pressurePlates);
        relativeTopBreakable.add(Material.SNOW);
        relativeTopBreakable.add(Material.REPEATER);
        relativeTopBreakable.add(Material.COMPARATOR);
        relativeTopBreakable.add(Material.TORCH);
        relativeTopBreakable.add(Material.REDSTONE_TORCH);
        relativeTopBreakable.addAll(woodenDoors);
        relativeTopBreakable.add(Material.IRON_DOOR);
        relativeTopBreakable.addAll(carpets);
        relativeTopBreakable.addAll(doublePlants);
        relativeTopBreakable.add(Material.BAMBOO);
        relativeTopBreakable.add(Material.BAMBOO_SAPLING);
        for (Material m : Material.values()) {
            if (m.name().startsWith("POTTED_")) {
                relativeTopBreakable.add(m);
            }
        }

        // Blocks that break falling entities
        fallingEntityKillers = EnumSet.noneOf(Material.class);
        fallingEntityKillers.add(Material.ACACIA_SIGN);
        fallingEntityKillers.add(Material.ACACIA_WALL_SIGN);
        fallingEntityKillers.add(Material.BIRCH_SIGN);
        fallingEntityKillers.add(Material.BIRCH_WALL_SIGN);
        fallingEntityKillers.add(Material.DARK_OAK_SIGN);
        fallingEntityKillers.add(Material.DARK_OAK_WALL_SIGN);
        fallingEntityKillers.add(Material.JUNGLE_SIGN);
        fallingEntityKillers.add(Material.JUNGLE_WALL_SIGN);
        fallingEntityKillers.add(Material.OAK_SIGN);
        fallingEntityKillers.add(Material.OAK_WALL_SIGN);
        fallingEntityKillers.add(Material.SPRUCE_SIGN);
        fallingEntityKillers.add(Material.SPRUCE_WALL_SIGN);
        fallingEntityKillers.addAll(pressurePlates);
        fallingEntityKillers.addAll(saplings);
        fallingEntityKillers.add(Material.DANDELION);
        fallingEntityKillers.add(Material.POPPY);
        fallingEntityKillers.add(Material.BLUE_ORCHID);
        fallingEntityKillers.add(Material.ALLIUM);
        fallingEntityKillers.add(Material.AZURE_BLUET);
        fallingEntityKillers.add(Material.ORANGE_TULIP);
        fallingEntityKillers.add(Material.WHITE_TULIP);
        fallingEntityKillers.add(Material.PINK_TULIP);
        fallingEntityKillers.add(Material.RED_TULIP);
        fallingEntityKillers.add(Material.OXEYE_DAISY);
        fallingEntityKillers.add(Material.BROWN_MUSHROOM);
        fallingEntityKillers.add(Material.RED_MUSHROOM);
        fallingEntityKillers.addAll(doublePlants);
        fallingEntityKillers.add(Material.WHEAT);
        fallingEntityKillers.add(Material.CARROT);
        fallingEntityKillers.add(Material.POTATO);
        fallingEntityKillers.add(Material.BEETROOT);
        fallingEntityKillers.add(Material.NETHER_WART);
        fallingEntityKillers.add(Material.COCOA);
        fallingEntityKillers.addAll(slabs);
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
        fallingEntityKillers.addAll(carpets);
        fallingEntityKillers.add(Material.PLAYER_HEAD);
        fallingEntityKillers.add(Material.PLAYER_WALL_HEAD);
        fallingEntityKillers.add(Material.CREEPER_HEAD);
        fallingEntityKillers.add(Material.CREEPER_WALL_HEAD);
        fallingEntityKillers.add(Material.DRAGON_HEAD);
        fallingEntityKillers.add(Material.DRAGON_WALL_HEAD);
        fallingEntityKillers.add(Material.ZOMBIE_HEAD);
        fallingEntityKillers.add(Material.ZOMBIE_WALL_HEAD);
        fallingEntityKillers.add(Material.SKELETON_SKULL);
        fallingEntityKillers.add(Material.SKELETON_WALL_SKULL);
        fallingEntityKillers.add(Material.WITHER_SKELETON_SKULL);
        fallingEntityKillers.add(Material.WITHER_SKELETON_WALL_SKULL);

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
        containerBlocks.add(Material.SHULKER_BOX);
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
        containerBlocks.add(Material.BARREL);
        containerBlocks.add(Material.BLAST_FURNACE);
        containerBlocks.add(Material.SMOKER);
        // Doesn't actually have a block inventory
        // containerBlocks.add(Material.ENDER_CHEST);

        // It doesn't seem like you could injure people with some of these, but they exist, so....
        projectileItems = new EnumMap<>(EntityType.class);
        projectileItems.put(EntityType.ARROW, Material.ARROW);
        projectileItems.put(EntityType.EGG, Material.EGG);
        projectileItems.put(EntityType.ENDER_PEARL, Material.ENDER_PEARL);
        projectileItems.put(EntityType.SMALL_FIREBALL, Material.FIRE_CHARGE); // Fire charge
        projectileItems.put(EntityType.FIREBALL, Material.FIRE_CHARGE); // Fire charge
        projectileItems.put(EntityType.FISHING_HOOK, Material.FISHING_ROD);
        projectileItems.put(EntityType.SNOWBALL, Material.SNOWBALL);
        projectileItems.put(EntityType.SPLASH_POTION, Material.SPLASH_POTION);
        projectileItems.put(EntityType.THROWN_EXP_BOTTLE, Material.EXPERIENCE_BOTTLE);
        projectileItems.put(EntityType.WITHER_SKULL, Material.WITHER_SKELETON_SKULL);
        projectileItems.put(EntityType.FIREWORK, Material.FIREWORK_ROCKET);

        nonFluidProofBlocks = EnumSet.noneOf(Material.class);
        nonFluidProofBlocks.addAll(singleBlockPlants);
        nonFluidProofBlocks.addAll(doublePlants);
        nonFluidProofBlocks.add(Material.REDSTONE_WALL_TORCH);
        nonFluidProofBlocks.add(Material.LEVER);
        nonFluidProofBlocks.add(Material.WALL_TORCH);
        nonFluidProofBlocks.add(Material.TRIPWIRE_HOOK);
        nonFluidProofBlocks.add(Material.COCOA);
        nonFluidProofBlocks.addAll(pressurePlates);
        nonFluidProofBlocks.addAll(saplings);
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
        nonFluidProofBlocks.addAll(carpets);

        bedBlocks = EnumSet.noneOf(Material.class);
        bedBlocks.add(Material.BLACK_BED);
        bedBlocks.add(Material.BLUE_BED);
        bedBlocks.add(Material.LIGHT_GRAY_BED);
        bedBlocks.add(Material.BROWN_BED);
        bedBlocks.add(Material.CYAN_BED);
        bedBlocks.add(Material.GRAY_BED);
        bedBlocks.add(Material.GREEN_BED);
        bedBlocks.add(Material.LIGHT_BLUE_BED);
        bedBlocks.add(Material.MAGENTA_BED);
        bedBlocks.add(Material.LIME_BED);
        bedBlocks.add(Material.ORANGE_BED);
        bedBlocks.add(Material.PINK_BED);
        bedBlocks.add(Material.PURPLE_BED);
        bedBlocks.add(Material.RED_BED);
        bedBlocks.add(Material.WHITE_BED);
        bedBlocks.add(Material.YELLOW_BED);

        concreteBlocks = EnumSet.noneOf(Material.class);
        concreteBlocks.add(Material.BLACK_CONCRETE);
        concreteBlocks.add(Material.BLUE_CONCRETE);
        concreteBlocks.add(Material.LIGHT_GRAY_CONCRETE);
        concreteBlocks.add(Material.BROWN_CONCRETE);
        concreteBlocks.add(Material.CYAN_CONCRETE);
        concreteBlocks.add(Material.GRAY_CONCRETE);
        concreteBlocks.add(Material.GREEN_CONCRETE);
        concreteBlocks.add(Material.LIGHT_BLUE_CONCRETE);
        concreteBlocks.add(Material.MAGENTA_CONCRETE);
        concreteBlocks.add(Material.LIME_CONCRETE);
        concreteBlocks.add(Material.ORANGE_CONCRETE);
        concreteBlocks.add(Material.PINK_CONCRETE);
        concreteBlocks.add(Material.PURPLE_CONCRETE);
        concreteBlocks.add(Material.RED_CONCRETE);
        concreteBlocks.add(Material.WHITE_CONCRETE);
        concreteBlocks.add(Material.YELLOW_CONCRETE);

        dyes = new EnumMap<>(Material.class);
        dyes.put(Material.BLACK_DYE, DyeColor.BLACK);
        dyes.put(Material.BLUE_DYE, DyeColor.BLUE);
        dyes.put(Material.LIGHT_GRAY_DYE, DyeColor.LIGHT_GRAY);
        dyes.put(Material.BROWN_DYE, DyeColor.BROWN);
        dyes.put(Material.CYAN_DYE, DyeColor.CYAN);
        dyes.put(Material.GRAY_DYE, DyeColor.GRAY);
        dyes.put(Material.GREEN_DYE, DyeColor.GREEN);
        dyes.put(Material.LIGHT_BLUE_DYE, DyeColor.LIGHT_BLUE);
        dyes.put(Material.MAGENTA_DYE, DyeColor.MAGENTA);
        dyes.put(Material.LIME_DYE, DyeColor.LIME);
        dyes.put(Material.ORANGE_DYE, DyeColor.ORANGE);
        dyes.put(Material.PINK_DYE, DyeColor.PINK);
        dyes.put(Material.PURPLE_DYE, DyeColor.PURPLE);
        dyes.put(Material.RED_DYE, DyeColor.RED);
        dyes.put(Material.WHITE_DYE, DyeColor.WHITE);
        dyes.put(Material.YELLOW_DYE, DyeColor.YELLOW);
    }

    private static final BlockFace[] relativeBlockFaces = new BlockFace[] {
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
        ArrayList<Location> blocks = new ArrayList<>();
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
        final ArrayList<ItemStack> diff = new ArrayList<>();
        for (ItemStack current : items2) {
            diff.add(new ItemStack(current));
        }
        for (ItemStack previous : items1) {
            boolean found = false;
            for (ItemStack current : diff) {
                if (current.isSimilar(previous)) {
                    int newAmount = current.getAmount() - previous.getAmount();
                    if (newAmount == 0) {
                        diff.remove(current);
                    } else {
                        current.setAmount(newAmount);
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                ItemStack subtracted = new ItemStack(previous);
                subtracted.setAmount(-subtracted.getAmount());
                diff.add(subtracted);
            }
        }
        return diff.toArray(new ItemStack[diff.size()]);
    }

    public static ItemStack[] compressInventory(ItemStack[] items) {
        final ArrayList<ItemStack> compressed = new ArrayList<>();
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

    public static boolean isConcreteBlock(Material m) {
        return concreteBlocks.contains(m);
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

    public static int saveSpawnHeight(Location loc) {
        final World world = loc.getWorld();
        world.getChunkAt(loc);
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

    public static boolean canFallIn(World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        Material mat = block.getType();
        if (canDirectlyFallIn(mat)) {
            return true;
        } else if (getFallingEntityKillers().contains(mat) || singleBlockPlants.contains(mat) || mat == Material.VINE) {
            if (slabs.contains(mat)) {
                if (((Slab) block.getBlockData()).getType() != Type.BOTTOM) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static boolean canDirectlyFallIn(Material m) {
        return isEmpty(m) || m == Material.WATER || m == Material.LAVA || m == Material.FIRE;
    }

    public static Material itemIDfromProjectileEntity(Entity e) {
        return projectileItems.get(e.getType());
    }

    public static boolean isDoublePlant(Material m) {
        return doublePlants.contains(m);
    }

    public static boolean isWoodenDoor(Material m) {
        return woodenDoors.contains(m);
    }

    public static boolean isButton(Material m) {
        return buttons.contains(m);
    }

    public static boolean isEmpty(Material m) {
        return m == Material.AIR || m == Material.CAVE_AIR || m == Material.VOID_AIR;
    }

    public static String toString(ItemStack stack) {
        if (stack == null || stack.getAmount() == 0 || isEmpty(stack.getType())) {
            return "nothing";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(stack.getAmount()).append("x ").append(stack.getType().name());
        ItemMeta meta = stack.getItemMeta();
        boolean metaStarted = false;
        if (meta.hasEnchants()) {
            Map<Enchantment, Integer> enchants = meta.getEnchants();
            if (!enchants.isEmpty()) {
                for (Entry<Enchantment, Integer> e : enchants.entrySet()) {
                    if (!metaStarted) {
                        sb.append(" [");
                        metaStarted = true;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(formatMinecraftKey(e.getKey().getKey().getKey()));
                    if (e.getValue().intValue() > 1) {
                        sb.append(" ").append(maybeToRoman(e.getValue().intValue() - 1));
                    }
                }
            }
        }
        if (meta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta emeta = (EnchantmentStorageMeta) meta;
            if (emeta.hasStoredEnchants()) {
                Map<Enchantment, Integer> enchants = emeta.getStoredEnchants();
                if (!enchants.isEmpty()) {
                    for (Entry<Enchantment, Integer> e : enchants.entrySet()) {
                        if (!metaStarted) {
                            sb.append(" [");
                            metaStarted = true;
                        } else {
                            sb.append(", ");
                        }
                        sb.append(formatMinecraftKey(e.getKey().getKey().getKey()));
                        if (e.getValue().intValue() > 1) {
                            sb.append(" ").append(maybeToRoman(e.getValue().intValue() - 1));
                        }
                    }
                }
            }
        }
        if (metaStarted) {
            sb.append("]");
        }
        return sb.toString();
    }

    private static final String[] romanNumbers = new String[] { "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "XI", "X" };

    private static String maybeToRoman(int value) {
        if (value > 0 && value <= 10) {
            return romanNumbers[value];
        }
        return Integer.toString(value);
    }

    public static String formatMinecraftKey(String s) {
        char[] cap = s.toCharArray();
        boolean lastSpace = true;
        for (int i = 0; i < cap.length; i++) {
            char c = cap[i];
            if (c == '_') {
                c = ' ';
                lastSpace = true;
            } else if (c >= '0' && c <= '9' || c == '(' || c == ')') {
                lastSpace = true;
            } else {
                if (lastSpace) {
                    c = Character.toUpperCase(c);
                } else {
                    c = Character.toLowerCase(c);
                }
                lastSpace = false;
            }
            cap[i] = c;
        }
        return new String(cap);
    }

    public static boolean isBed(Material type) {
        return bedBlocks.contains(type);
    }

    public static boolean isDye(Material type) {
        return dyes.containsKey(type);
    }

    public static DyeColor dyeToDyeColor(Material type) {
        return dyes.get(type);
    }

    public static Block getConnectedChest(Block chestBlock) {
        // is this a chest?
        BlockData blockData = chestBlock.getBlockData();
        if (!(blockData instanceof org.bukkit.block.data.type.Chest)) {
            return null;
        }
        // so check if is should have a neighbour
        org.bukkit.block.data.type.Chest chestData = (org.bukkit.block.data.type.Chest) blockData;
        org.bukkit.block.data.type.Chest.Type chestType = chestData.getType();
        if (chestType != org.bukkit.block.data.type.Chest.Type.SINGLE) {
            // check if the neighbour exists
            BlockFace chestFace = chestData.getFacing();
            BlockFace faceToSecondChest;
            if (chestFace == BlockFace.WEST) {
                faceToSecondChest = BlockFace.NORTH;
            } else if (chestFace == BlockFace.NORTH) {
                faceToSecondChest = BlockFace.EAST;
            } else if (chestFace == BlockFace.EAST) {
                faceToSecondChest = BlockFace.SOUTH;
            } else if (chestFace == BlockFace.SOUTH) {
                faceToSecondChest = BlockFace.WEST;
            } else {
                return null;
            }
            org.bukkit.block.data.type.Chest.Type wantedChestType = org.bukkit.block.data.type.Chest.Type.RIGHT;
            if (chestType == org.bukkit.block.data.type.Chest.Type.RIGHT) {
                faceToSecondChest = faceToSecondChest.getOppositeFace();
                wantedChestType = org.bukkit.block.data.type.Chest.Type.LEFT;
            }
            Block face = chestBlock.getRelative(faceToSecondChest);
            if (face.getType() == chestBlock.getType()) {
                // check is the neighbour connects to this chest
                org.bukkit.block.data.type.Chest otherChestData = (org.bukkit.block.data.type.Chest) face.getBlockData();
                if (otherChestData.getType() != wantedChestType || otherChestData.getFacing() != chestFace) {
                    return null;
                }
                return face;
            }
        }
        return null;
    }

    public static Entity loadEntityAround(Chunk chunk, UUID uuid) {
        Entity e = Bukkit.getEntity(uuid);
        if (e != null) {
            return e;
        }
        if (!chunk.isLoaded()) {
            chunk.getWorld().getChunkAt(chunk.getX(), chunk.getZ());
            e = Bukkit.getEntity(uuid);
            if (e != null) {
                return e;
            }
        }
        int chunkx = chunk.getX();
        int chunkz = chunk.getZ();
        for (int i = 0; i < 8; i++) {
            int x = i < 3 ? chunkx - 1 : (i < 5 ? chunkx : chunkx + 1);
            int z = i == 0 || i == 3 || i == 5 ? chunkz - 1 : (i == 1 || i == 6 ? chunkz : chunkz + 1);
            if (!chunk.getWorld().isChunkLoaded(x, z)) {
                chunk.getWorld().getChunkAt(x, z);
                e = Bukkit.getEntity(uuid);
                if (e != null) {
                    return e;
                }
            }
        }
        return null;
    }

    private static final HashMap<String, EntityType> types = new HashMap<>();
    static {
        for (EntityType t : EntityType.values()) {
            types.put(t.name().toLowerCase(), t);
            @SuppressWarnings("deprecation")
            String typeName = t.getName();
            if (typeName != null) {
                types.put(typeName.toLowerCase(), t);
            }
            Class<? extends Entity> ec = t.getEntityClass();
            if (ec != null) {
                types.put(ec.getSimpleName().toLowerCase(), t);
            }
        }
    }

    public static EntityType matchEntityType(String typeName) {
        return types.get(typeName.toLowerCase());
    }

    public static ItemStack getItemInSlot(ArmorStand stand, EquipmentSlot slot) {
        if (slot == EquipmentSlot.HAND) {
            return stand.getEquipment().getItemInMainHand();
        } else if (slot == EquipmentSlot.OFF_HAND) {
            return stand.getEquipment().getItemInOffHand();
        } else if (slot == EquipmentSlot.FEET) {
            return stand.getEquipment().getBoots();
        } else if (slot == EquipmentSlot.LEGS) {
            return stand.getEquipment().getLeggings();
        } else if (slot == EquipmentSlot.CHEST) {
            return stand.getEquipment().getChestplate();
        } else if (slot == EquipmentSlot.HEAD) {
            return stand.getEquipment().getHelmet();
        }
        return null;
    }

    public static void setItemInSlot(ArmorStand stand, EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.HAND) {
            stand.getEquipment().setItemInMainHand(stack);
        } else if (slot == EquipmentSlot.OFF_HAND) {
            stand.getEquipment().setItemInOffHand(stack);
        } else if (slot == EquipmentSlot.FEET) {
            stand.getEquipment().setBoots(stack);
        } else if (slot == EquipmentSlot.LEGS) {
            stand.getEquipment().setLeggings(stack);
        } else if (slot == EquipmentSlot.CHEST) {
            stand.getEquipment().setChestplate(stack);
        } else if (slot == EquipmentSlot.HEAD) {
            stand.getEquipment().setHelmet(stack);
        }
    }

    public static ItemStack[] deepCopy(ItemStack[] of) {
        ItemStack[] result = new ItemStack[of.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = of[i] == null ? null : new ItemStack(of[i]);
        }
        return result;
    }

    private static int getFirstPartialItemStack(ItemStack item, ItemStack[] contents, int start) {
        for (int i = start; i < contents.length; i++) {
            ItemStack content = contents[i];
            if (content != null && content.isSimilar(item) && content.getAmount() < content.getMaxStackSize()) {
                return i;
            }
        }
        return -1;
    }

    private static int getFirstFreeItemStack(ItemStack[] contents, int start) {
        for (int i = start; i < contents.length; i++) {
            ItemStack content = contents[i];
            if (content == null || content.getAmount() == 0 || content.getType() == Material.AIR) {
                return i;
            }
        }
        return -1;
    }

    public static boolean hasInventoryStorageSpaceFor(Inventory inv, ItemStack... items) {
        ItemStack[] contents = deepCopy(inv.getStorageContents());
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                int remaining = item.getAmount();
                // fill partial stacks
                int firstPartial = -1;
                while (remaining > 0) {
                    firstPartial = getFirstPartialItemStack(item, contents, firstPartial + 1);
                    if (firstPartial < 0) {
                        break;
                    }
                    ItemStack content = contents[firstPartial];
                    int add = Math.min(content.getMaxStackSize() - content.getAmount(), remaining);
                    content.setAmount(content.getAmount() + add);
                    remaining -= add;
                }
                // create new stacks
                int firstFree = -1;
                while (remaining > 0) {
                    firstFree = getFirstFreeItemStack(contents, firstFree + 1);
                    if (firstFree < 0) {
                        return false; // no free place found
                    }
                    ItemStack content = new ItemStack(item);
                    contents[firstFree] = content;
                    // max stack size might return -1, in this case assume 1
                    int add = Math.min(Math.max(content.getMaxStackSize(), 1), remaining);
                    content.setAmount(add);
                    remaining -= add;
                }
            }
        }
        return true;
    }
}
