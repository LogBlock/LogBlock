package de.diddiz.LogBlock.util;

import static de.diddiz.LogBlock.util.MessagingUtil.prettyMaterial;

import de.diddiz.LogBlock.LogBlock;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.Set;
import java.util.UUID;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.ItemTag;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Item;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.HangingSign;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Slab.Type;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.WallHangingSign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class BukkitUtils {

    private static final Map<Material, DyeColor> dyes;
    private static final Map<EntityType, Material> projectileItems;

    private static final Set<Material> alwaysWaterlogged;
    private static final Set<Material> concreteBlocks;
    private static final Set<Material> containerBlocks;
    private static final Set<Material> doublePlants;
    private static final Set<Material> fallingEntityKillers;
    private static final Set<Material> nonFluidProofBlocks;
    private static final Set<Material> relativeBreakable;
    private static final Set<Material> relativeTopBreakable;
    private static final Set<Material> singleBlockPlants;

    private static final Tag<Material> allSigns;
    private static final Tag<Material> bedBlocks;
    private static final Tag<Material> buttons;
    private static final Tag<Material> candleCakes;
    private static final Tag<Material> candles;
    private static final Tag<Material> cropBlocks;
    private static final Tag<Material> fenceGates;
    private static final Tag<Material> hangingSigns;
    private static final Tag<Material> pressurePlates;
    private static final Tag<Material> shulkerBoxBlocks;
    private static final Tag<Material> slabs;
    private static final Tag<Material> woodenDoors;
    private static final Tag<Material> woodenTrapdoors;

    static {
        // Global Tags

        // https://minecraft.fandom.com/wiki/Tag#blocks_fence_gates
        fenceGates = Tag.FENCE_GATES;

        // https://minecraft.fandom.com/wiki/Tag#blocks_wooden_trapdoors
        woodenTrapdoors = Tag.WOODEN_TRAPDOORS;

        // https://minecraft.fandom.com/wiki/Tag#blocks_pressure_plates
        pressurePlates = Tag.PRESSURE_PLATES;

        // https://minecraft.fandom.com/wiki/Tag#blocks_wooden_doors
        woodenDoors = Tag.WOODEN_DOORS;

        // https://minecraft.fandom.com/wiki/Tag#blocks_slabs
        slabs = Tag.SLABS;

        // https://minecraft.fandom.com/wiki/Tag#blocks_buttons
        buttons = Tag.BUTTONS;

        // https://minecraft.fandom.com/wiki/Tag#blocks_ceiling_hanging_signs
        hangingSigns = Tag.CEILING_HANGING_SIGNS;

        // https://minecraft.fandom.com/wiki/Tag#blocks_all_signs
        allSigns = Tag.ALL_SIGNS;

        // https://minecraft.fandom.com/wiki/Tag#blocks_candles
        candles = Tag.CANDLES;

        // https://minecraft.fandom.com/wiki/Tag#blocks_candle_cakes
        candleCakes = Tag.CANDLE_CAKES;

        // https://minecraft.fandom.com/wiki/Tag#blocks_crops
        cropBlocks = Tag.CROPS;

        // https://minecraft.fandom.com/wiki/Tag#blocks_shulker_boxes
        shulkerBoxBlocks = Tag.SHULKER_BOXES;

        // https://minecraft.fandom.com/wiki/Tag#blocks_beds
        bedBlocks = Tag.BEDS;

        // Local Tags

        // https://minecraft.fandom.com/wiki/Tag#blocks_standing_signs
        Set<Material> signs = Tag.STANDING_SIGNS.getValues();

        // https://minecraft.fandom.com/wiki/Tag#blocks_wall_signs
        Set<Material> wallSigns = Tag.WALL_SIGNS.getValues();

        // https://minecraft.fandom.com/wiki/Tag#blocks_wool_carpets
        Set<Material> carpets = Tag.WOOL_CARPETS.getValues();

        // https://minecraft.fandom.com/wiki/Tag#blocks_flower_pots
        Set<Material> flowerPots = Tag.FLOWER_POTS.getValues();

        // https://minecraft.fandom.com/wiki/Tag#blocks_saplings
        Set<Material> saplings = Tag.SAPLINGS.getValues();

        // https://minecraft.fandom.com/wiki/Tag#blocks_small_flowers
        Set<Material> smallFlowers = Tag.SMALL_FLOWERS.getValues();

        Set<Material> tallFlowers = Set.of(Material.SUNFLOWER,
                Material.LILAC,
                Material.PEONY,
                Material.ROSE_BUSH,
                Material.PITCHER_PLANT);

        Set<Material> bannerStanding = Set.of(Material.WHITE_BANNER,
                Material.ORANGE_BANNER,
                Material.MAGENTA_BANNER,
                Material.LIGHT_BLUE_BANNER,
                Material.YELLOW_BANNER,
                Material.LIME_BANNER,
                Material.PINK_BANNER,
                Material.GRAY_BANNER,
                Material.LIGHT_GRAY_BANNER,
                Material.CYAN_BANNER,
                Material.PURPLE_BANNER,
                Material.BLUE_BANNER,
                Material.BROWN_BANNER,
                Material.GREEN_BANNER,
                Material.RED_BANNER,
                Material.BLACK_BANNER);

        Set<Material> bannerWall = Set.of(Material.WHITE_WALL_BANNER,
                Material.ORANGE_WALL_BANNER,
                Material.MAGENTA_WALL_BANNER,
                Material.LIGHT_BLUE_WALL_BANNER,
                Material.YELLOW_WALL_BANNER,
                Material.LIME_WALL_BANNER,
                Material.PINK_WALL_BANNER,
                Material.GRAY_WALL_BANNER,
                Material.LIGHT_GRAY_WALL_BANNER,
                Material.CYAN_WALL_BANNER,
                Material.PURPLE_WALL_BANNER,
                Material.BLUE_WALL_BANNER,
                Material.BROWN_WALL_BANNER,
                Material.GREEN_WALL_BANNER,
                Material.RED_WALL_BANNER,
                Material.BLACK_WALL_BANNER);

        Set<Material> bannerAll = Tag.BANNERS.getValues();

        Set<Material> headAndSkulls = Set.of(Material.PLAYER_HEAD,
                Material.PLAYER_WALL_HEAD,
                Material.CREEPER_HEAD,
                Material.CREEPER_WALL_HEAD,
                Material.DRAGON_HEAD,
                Material.DRAGON_WALL_HEAD,
                Material.ZOMBIE_HEAD,
                Material.ZOMBIE_WALL_HEAD,
                Material.SKELETON_SKULL,
                Material.SKELETON_WALL_SKULL,
                Material.WITHER_SKELETON_SKULL,
                Material.WITHER_SKELETON_WALL_SKULL,
                Material.PIGLIN_HEAD,
                Material.PIGLIN_WALL_HEAD);

        Set<Material> standingTorch = Set.of(Material.TORCH,
                Material.SOUL_TORCH,
                Material.REDSTONE_TORCH);

        Set<Material> wallTorch = Set.of(Material.WALL_TORCH,
                Material.SOUL_WALL_TORCH,
                Material.REDSTONE_WALL_TORCH);

        singleBlockPlants = new HashSet<>();
        singleBlockPlants.addAll(smallFlowers);
        singleBlockPlants.add(Material.SHORT_GRASS);
        singleBlockPlants.add(Material.FERN);
        singleBlockPlants.add(Material.DEAD_BUSH);
        singleBlockPlants.add(Material.BROWN_MUSHROOM);
        singleBlockPlants.add(Material.RED_MUSHROOM);
        singleBlockPlants.add(Material.SWEET_BERRY_BUSH);
        singleBlockPlants.add(Material.CRIMSON_FUNGUS);
        singleBlockPlants.add(Material.WARPED_FUNGUS);
        singleBlockPlants.add(Material.CRIMSON_ROOTS);
        singleBlockPlants.add(Material.WARPED_ROOTS);
        singleBlockPlants.add(Material.NETHER_SPROUTS);
        singleBlockPlants.add(Material.AZALEA);
        singleBlockPlants.add(Material.FLOWERING_AZALEA);
        singleBlockPlants.add(Material.PINK_PETALS);
        singleBlockPlants.add(Material.PITCHER_CROP);

        doublePlants = new HashSet<>();
        doublePlants.addAll(tallFlowers);
        doublePlants.add(Material.TALL_GRASS);
        doublePlants.add(Material.LARGE_FERN);
        doublePlants.add(Material.TALL_SEAGRASS);
        doublePlants.add(Material.SMALL_DRIPLEAF);

        // Blocks that break when they are attached to a block
        relativeBreakable = new HashSet<>();
        relativeBreakable.addAll(bannerWall);
        relativeBreakable.addAll(buttons.getValues());
        relativeBreakable.addAll(wallSigns);
        relativeBreakable.addAll(wallTorch);
        relativeBreakable.add(Material.LADDER);
        relativeBreakable.add(Material.LEVER);
        relativeBreakable.add(Material.TRIPWIRE_HOOK);
        relativeBreakable.add(Material.COCOA);
        relativeBreakable.add(Material.BELL);
        relativeBreakable.add(Material.AMETHYST_CLUSTER);
        relativeBreakable.add(Material.SMALL_AMETHYST_BUD);
        relativeBreakable.add(Material.MEDIUM_AMETHYST_BUD);
        relativeBreakable.add(Material.LARGE_AMETHYST_BUD);

        // Blocks that break when they are on top of a block
        relativeTopBreakable = new HashSet<>();
        relativeTopBreakable.addAll(bannerStanding);
        relativeTopBreakable.addAll(candleCakes.getValues());
        relativeTopBreakable.addAll(candles.getValues());
        relativeTopBreakable.addAll(carpets);
        relativeTopBreakable.addAll(cropBlocks.getValues());
        relativeTopBreakable.addAll(doublePlants);
        relativeTopBreakable.addAll(flowerPots);
        relativeTopBreakable.addAll(pressurePlates.getValues());
        relativeTopBreakable.addAll(saplings);
        relativeTopBreakable.addAll(signs);
        relativeTopBreakable.addAll(singleBlockPlants);
        relativeTopBreakable.addAll(standingTorch);
        relativeTopBreakable.addAll(woodenDoors.getValues());
        relativeTopBreakable.add(Material.LILY_PAD);
        relativeTopBreakable.add(Material.CACTUS);
        relativeTopBreakable.add(Material.SUGAR_CANE);
        relativeTopBreakable.add(Material.FLOWER_POT);
        relativeTopBreakable.add(Material.POWERED_RAIL);
        relativeTopBreakable.add(Material.DETECTOR_RAIL);
        relativeTopBreakable.add(Material.ACTIVATOR_RAIL);
        relativeTopBreakable.add(Material.RAIL);
        relativeTopBreakable.add(Material.REDSTONE_WIRE);
        relativeTopBreakable.add(Material.SNOW);
        relativeTopBreakable.add(Material.REPEATER);
        relativeTopBreakable.add(Material.COMPARATOR);
        relativeTopBreakable.add(Material.IRON_DOOR);
        relativeTopBreakable.add(Material.BAMBOO);
        relativeTopBreakable.add(Material.BAMBOO_SAPLING);
        relativeTopBreakable.add(Material.TWISTING_VINES);
        relativeTopBreakable.add(Material.TWISTING_VINES_PLANT);
        relativeTopBreakable.add(Material.BIG_DRIPLEAF);
        relativeTopBreakable.add(Material.BIG_DRIPLEAF_STEM);

        // Blocks that break falling entities
        fallingEntityKillers = new HashSet<>();
        fallingEntityKillers.addAll(bannerAll);
        fallingEntityKillers.addAll(candleCakes.getValues());
        fallingEntityKillers.addAll(candles.getValues());
        fallingEntityKillers.addAll(carpets);
        fallingEntityKillers.addAll(cropBlocks.getValues());
        fallingEntityKillers.addAll(doublePlants);
        fallingEntityKillers.addAll(pressurePlates.getValues());
        fallingEntityKillers.addAll(saplings);
        fallingEntityKillers.addAll(signs);
        fallingEntityKillers.addAll(singleBlockPlants);
        fallingEntityKillers.addAll(headAndSkulls);
        fallingEntityKillers.addAll(slabs.getValues());
        fallingEntityKillers.addAll(standingTorch);
        fallingEntityKillers.addAll(wallSigns);
        fallingEntityKillers.addAll(wallTorch);
        fallingEntityKillers.add(Material.NETHER_WART);
        fallingEntityKillers.add(Material.COCOA);
        fallingEntityKillers.add(Material.FLOWER_POT);
        fallingEntityKillers.add(Material.POWERED_RAIL);
        fallingEntityKillers.add(Material.DETECTOR_RAIL);
        fallingEntityKillers.add(Material.ACTIVATOR_RAIL);
        fallingEntityKillers.add(Material.RAIL);
        fallingEntityKillers.add(Material.LEVER);
        fallingEntityKillers.add(Material.REDSTONE_WIRE);
        fallingEntityKillers.add(Material.REPEATER);
        fallingEntityKillers.add(Material.COMPARATOR);
        fallingEntityKillers.add(Material.DAYLIGHT_DETECTOR);
        fallingEntityKillers.remove(Material.SHORT_GRASS);
        fallingEntityKillers.remove(Material.NETHER_SPROUTS);

        // Container Blocks
        containerBlocks = new HashSet<>();
        containerBlocks.addAll(shulkerBoxBlocks.getValues());
        containerBlocks.add(Material.CHEST);
        containerBlocks.add(Material.TRAPPED_CHEST);
        containerBlocks.add(Material.DISPENSER);
        containerBlocks.add(Material.DROPPER);
        containerBlocks.add(Material.HOPPER);
        containerBlocks.add(Material.BREWING_STAND);
        containerBlocks.add(Material.FURNACE);
        containerBlocks.add(Material.BARREL);
        containerBlocks.add(Material.BLAST_FURNACE);
        containerBlocks.add(Material.SMOKER);
        containerBlocks.add(Material.CHISELED_BOOKSHELF);
        containerBlocks.add(Material.DECORATED_POT);
        // Doesn't actually have a block inventory
        // containerBlocks.add(Material.ENDER_CHEST);

        // It doesn't seem like you could injure people with some of these, but they exist, so....
        projectileItems = new HashMap<>();
        projectileItems.put(EntityType.ARROW, Material.ARROW);
        projectileItems.put(EntityType.EGG, Material.EGG);
        projectileItems.put(EntityType.ENDER_PEARL, Material.ENDER_PEARL);
        projectileItems.put(EntityType.SMALL_FIREBALL, Material.FIRE_CHARGE); // Fire charge
        projectileItems.put(EntityType.FIREBALL, Material.FIRE_CHARGE); // Fire charge
        projectileItems.put(EntityType.FISHING_BOBBER, Material.FISHING_ROD);
        projectileItems.put(EntityType.SNOWBALL, Material.SNOWBALL);
        projectileItems.put(EntityType.SPLASH_POTION, Material.SPLASH_POTION);
        projectileItems.put(EntityType.LINGERING_POTION, Material.LINGERING_POTION);
        projectileItems.put(EntityType.EXPERIENCE_BOTTLE, Material.EXPERIENCE_BOTTLE);
        projectileItems.put(EntityType.WITHER_SKULL, Material.WITHER_SKELETON_SKULL);
        projectileItems.put(EntityType.FIREWORK_ROCKET, Material.FIREWORK_ROCKET);

        nonFluidProofBlocks = new HashSet<>();
        nonFluidProofBlocks.addAll(carpets);
        nonFluidProofBlocks.addAll(cropBlocks.getValues());
        nonFluidProofBlocks.addAll(doublePlants);
        nonFluidProofBlocks.addAll(headAndSkulls);
        nonFluidProofBlocks.addAll(pressurePlates.getValues());
        nonFluidProofBlocks.addAll(saplings);
        nonFluidProofBlocks.addAll(singleBlockPlants);
        nonFluidProofBlocks.addAll(standingTorch);
        nonFluidProofBlocks.addAll(wallTorch);
        nonFluidProofBlocks.add(Material.LEVER);
        nonFluidProofBlocks.add(Material.TRIPWIRE_HOOK);
        nonFluidProofBlocks.add(Material.COCOA);
        nonFluidProofBlocks.add(Material.NETHER_WART);
        nonFluidProofBlocks.add(Material.FLOWER_POT);
        // nonFluidProofBlocks.add(Material.POWERED_RAIL);
        // nonFluidProofBlocks.add(Material.DETECTOR_RAIL);
        // nonFluidProofBlocks.add(Material.ACTIVATOR_RAIL);
        // nonFluidProofBlocks.add(Material.RAIL);
        nonFluidProofBlocks.add(Material.LEVER);
        nonFluidProofBlocks.add(Material.REDSTONE_WIRE);
        nonFluidProofBlocks.add(Material.REPEATER);
        nonFluidProofBlocks.add(Material.COMPARATOR);
        nonFluidProofBlocks.add(Material.DAYLIGHT_DETECTOR);

        alwaysWaterlogged = Set.of(Material.SEAGRASS,
                Material.TALL_SEAGRASS,
                Material.KELP,
                Material.KELP_PLANT);

        concreteBlocks = Set.of(Material.BLACK_CONCRETE,
                Material.BLUE_CONCRETE,
                Material.LIGHT_GRAY_CONCRETE,
                Material.BROWN_CONCRETE,
                Material.CYAN_CONCRETE,
                Material.GRAY_CONCRETE,
                Material.GREEN_CONCRETE,
                Material.LIGHT_BLUE_CONCRETE,
                Material.MAGENTA_CONCRETE,
                Material.LIME_CONCRETE,
                Material.ORANGE_CONCRETE,
                Material.PINK_CONCRETE,
                Material.PURPLE_CONCRETE,
                Material.RED_CONCRETE,
                Material.WHITE_CONCRETE,
                Material.YELLOW_CONCRETE);

        dyes = new HashMap<>();
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
            try {
                diff.add(new ItemStack(current));
            } catch (NullPointerException e) {
                LogBlock.getInstance().getLogger().log(Level.SEVERE, "Could not clone ItemStack, probably Spigot bug SPIGOT-6025", e); // SPIGOT-6025
            }
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
                try {
                    ItemStack subtracted = new ItemStack(previous);
                    subtracted.setAmount(-subtracted.getAmount());
                    diff.add(subtracted);
                } catch (NullPointerException e) {
                    LogBlock.getInstance().getLogger().log(Level.SEVERE, "Could not clone ItemStack, probably Spigot bug SPIGOT-6025", e); // SPIGOT-6025
                }
            }
        }
        return diff.toArray(new ItemStack[diff.size()]);
    }

    public static Collection<ItemStackAndAmount> compressInventory(ItemStack[] items) {
        final HashMap<ItemStack, Integer> compressed = new HashMap<>();
        for (final ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                int amount = item.getAmount();
                ItemStack stack = item.clone();
                stack.setAmount(1);
                Integer old = compressed.get(stack);
                compressed.put(stack, (old == null ? 0 : old) + amount);
            }
        }
        ArrayList<ItemStackAndAmount> result = new ArrayList<>();
        for (Entry<ItemStack, Integer> e : compressed.entrySet()) {
            result.add(new ItemStackAndAmount(e.getKey(), e.getValue()));
        }
        return result;
    }

    public static String friendlyWorldname(String worldName) {
        return new File(worldName).getName();
    }

    public static Set<Material> getRelativeBreakables() {
        return Collections.unmodifiableSet(relativeBreakable);
    }

    public static boolean isRelativeTopBreakable(Material type) {
        return relativeTopBreakable.contains(type);
    }

    public static boolean isFallingEntityKiller(Material type) {
        return fallingEntityKillers.contains(type);
    }

    public static boolean isNonFluidProofBlock(Material type) {
        return nonFluidProofBlocks.contains(type);
    }

    public static boolean isCropBlock(Material type) {
        return cropBlocks.isTagged(type);
    }

    public static boolean isContainerBlock(Material type) {
        return containerBlocks.contains(type);
    }

    public static Set<Material> getShulkerBoxBlocks() {
        return shulkerBoxBlocks.getValues(); // Already an unmodifiable Set
    }

    public static boolean isShulkerBoxBlock(Material type) {
        return shulkerBoxBlocks.isTagged(type);
    }

    public static boolean isConcreteBlock(Material m) {
        return concreteBlocks.contains(m);
    }

    public static String entityName(Entity entity) {
        if (entity instanceof Player player) {
            return player.getName();
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

    public static int safeSpawnHeight(Location loc) {
        final World world = loc.getWorld();
        world.getChunkAt(loc);
        final int x = loc.getBlockX(), z = loc.getBlockZ();
        int y = loc.getBlockY();
        boolean lower = world.getBlockAt(x, y, z).isEmpty(), upper = world.getBlockAt(x, y + 1, z).isEmpty();
        while ((!lower || !upper) && y != world.getMaxHeight()) {
            lower = upper;
            upper = world.getBlockAt(x, ++y, z).isEmpty();
        }
        while (world.getBlockAt(x, y - 1, z).isEmpty() && y != world.getMinHeight()) {
            y--;
        }
        return y;
    }

    public static int modifyContainer(BlockState b, ItemStackAndAmount item, boolean remove) {
        if (item.amount() > 0 && b instanceof InventoryHolder c) {
            final Inventory inv = c.getInventory();
            if (remove) {
                return InventoryUtils.removeFromInventory(inv, item);
            } else {
                return InventoryUtils.addToInventory(inv, item);
            }
        }
        return 0;
    }

    public static boolean canFallIn(World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        Material mat = block.getType();
        if (canDirectlyFallIn(mat)) {
            return true;
        } else if (isFallingEntityKiller(mat) || singleBlockPlants.contains(mat) || mat == Material.VINE) {
            if (slabs.isTagged(mat)) {
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
        return woodenDoors.isTagged(m);
    }

    public static boolean isButton(Material m) {
        return buttons.isTagged(m);
    }

    public static boolean isEmpty(Material m) {
        return m == Material.AIR || m == Material.CAVE_AIR || m == Material.VOID_AIR;
    }

    public static TextComponent toString(ItemStackAndAmount stack) {
        if (stack == null || stack.stack() == null || stack.amount() == 0 || isEmpty(stack.stack().getType())) {
            return prettyMaterial("nothing");
        }
        TextComponent msg = MessagingUtil.createTextComponentWithColor(stack.amount() + "x ", TypeColor.DEFAULT.getColor());
        msg.addExtra(prettyMaterial(stack.stack().getType()));

        try {
            String itemTag = stack.stack().getItemMeta().getAsString();
            msg.setHoverEvent(new HoverEvent(Action.SHOW_ITEM, new Item(stack.stack().getType().getKey().toString(), 1, itemTag != null ? ItemTag.ofNbt(itemTag) : null)));
        } catch (Exception e) {
            LogBlock.getInstance().getLogger().log(Level.SEVERE, "Failed to convert Itemstack to JSON", e);
            msg.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new Text(new BaseComponent[] { MessagingUtil.createTextComponentWithColor("Error", TypeColor.ERROR.getColor()) })));
        }

        return msg;
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
        return bedBlocks.isTagged(type);
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
            if (t != EntityType.UNKNOWN) {
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
                types.put(t.getKey().getKey(), t);
                types.put(t.getKey().toString(), t);
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

    public static boolean isSimilarForRollback(Material expected, Material found) {
        if (expected == found) {
            return true;
        }
        if (expected == Material.DIRT || expected == Material.MYCELIUM || expected == Material.FARMLAND || expected == Material.GRASS_BLOCK || expected == Material.PODZOL || expected == Material.DIRT_PATH) {
            return found == Material.DIRT || found == Material.MYCELIUM || found == Material.FARMLAND || found == Material.GRASS_BLOCK || found == Material.PODZOL || found == Material.DIRT_PATH;
        }
        if (expected == Material.BAMBOO || expected == Material.BAMBOO_SAPLING) {
            return found == Material.BAMBOO || found == Material.BAMBOO_SAPLING;
        }
        if (expected == Material.SPONGE || expected == Material.WET_SPONGE) {
            return found == Material.SPONGE || found == Material.WET_SPONGE;
        }
        if (expected == Material.MELON_STEM || expected == Material.ATTACHED_MELON_STEM) {
            return found == Material.MELON_STEM || found == Material.ATTACHED_MELON_STEM;
        }
        if (expected == Material.PUMPKIN_STEM || expected == Material.ATTACHED_PUMPKIN_STEM) {
            return found == Material.PUMPKIN_STEM || found == Material.ATTACHED_PUMPKIN_STEM;
        }
        if (expected == Material.TWISTING_VINES || expected == Material.TWISTING_VINES_PLANT) {
            return found == Material.TWISTING_VINES || found == Material.TWISTING_VINES_PLANT;
        }
        if (expected == Material.WEEPING_VINES || expected == Material.WEEPING_VINES_PLANT) {
            return found == Material.WEEPING_VINES || found == Material.WEEPING_VINES_PLANT;
        }
        if (expected == Material.CAVE_VINES || expected == Material.CAVE_VINES_PLANT) {
            return found == Material.CAVE_VINES || found == Material.CAVE_VINES_PLANT;
        }
        if (expected == Material.BIG_DRIPLEAF || expected == Material.BIG_DRIPLEAF_STEM) {
            return found == Material.BIG_DRIPLEAF || found == Material.BIG_DRIPLEAF_STEM;
        }
        if (expected == Material.COPPER_BLOCK || expected == Material.EXPOSED_COPPER || expected == Material.WEATHERED_COPPER || expected == Material.OXIDIZED_COPPER) {
            return found == Material.COPPER_BLOCK || found == Material.EXPOSED_COPPER || found == Material.WEATHERED_COPPER || found == Material.OXIDIZED_COPPER;
        }
        if (expected == Material.CUT_COPPER || expected == Material.EXPOSED_CUT_COPPER || expected == Material.WEATHERED_CUT_COPPER || expected == Material.OXIDIZED_CUT_COPPER) {
            return found == Material.CUT_COPPER || found == Material.EXPOSED_CUT_COPPER || found == Material.WEATHERED_CUT_COPPER || found == Material.OXIDIZED_CUT_COPPER;
        }
        if (expected == Material.CUT_COPPER_STAIRS || expected == Material.EXPOSED_CUT_COPPER_STAIRS || expected == Material.WEATHERED_CUT_COPPER_STAIRS || expected == Material.OXIDIZED_CUT_COPPER_STAIRS) {
            return found == Material.CUT_COPPER_STAIRS || found == Material.EXPOSED_CUT_COPPER_STAIRS || found == Material.WEATHERED_CUT_COPPER_STAIRS || found == Material.OXIDIZED_CUT_COPPER_STAIRS;
        }
        if (expected == Material.CUT_COPPER_SLAB || expected == Material.EXPOSED_CUT_COPPER_SLAB || expected == Material.WEATHERED_CUT_COPPER_SLAB || expected == Material.OXIDIZED_CUT_COPPER_SLAB) {
            return found == Material.CUT_COPPER_SLAB || found == Material.EXPOSED_CUT_COPPER_SLAB || found == Material.WEATHERED_CUT_COPPER_SLAB || found == Material.OXIDIZED_CUT_COPPER_SLAB;
        }
        return false;
    }

    public static Set<Material> getAllSignMaterials() {
        return allSigns.getValues();
    }

    public static boolean isAlwaysWaterlogged(Material m) {
        return alwaysWaterlogged.contains(m);
    }

    public static boolean isCandle(Material m) {
        return candles.isTagged(m);
    }

    public static boolean isCandleCake(Material m) {
        return candleCakes.isTagged(m);
    }

    public static boolean isHangingSign(Material m) {
        return hangingSigns.isTagged(m);
    }

    public static boolean isFenceGate(Material m) {
        return fenceGates.isTagged(m);
    }

    public static boolean isWoodenTrapdoor(Material m) {
        return woodenTrapdoors.isTagged(m);
    }

    public static boolean isPressurePlate(Material m) {
        return pressurePlates.isTagged(m);
    }

    public static boolean isSign(Material m) {
        return allSigns.isTagged(m);
    }

    public static Side getFacingSignSide(Entity entity, Block sign) {
        BlockData data = sign.getBlockData();
        Material type = data.getMaterial();
        BlockFace signFace = null;
        double centerx = 0.5;
        double centerz = 0.5;
        double yRotationDegree = 0;
        if (type.data == Sign.class || type.data == HangingSign.class) {
            Rotatable rotatableData = (Rotatable) data;
            signFace = rotatableData.getRotation();
            if (signFace == BlockFace.SOUTH) {
                yRotationDegree = 360 * 0.0 / 16.0;
            } else if (signFace == BlockFace.SOUTH_SOUTH_WEST) {
                yRotationDegree = 360 * 1.0 / 16.0;
            } else if (signFace == BlockFace.SOUTH_WEST) {
                yRotationDegree = 360 * 2.0 / 16.0;
            } else if (signFace == BlockFace.WEST_SOUTH_WEST) {
                yRotationDegree = 360 * 3.0 / 16.0;
            } else if (signFace == BlockFace.WEST) {
                yRotationDegree = 360 * 4.0 / 16.0;
            } else if (signFace == BlockFace.WEST_NORTH_WEST) {
                yRotationDegree = 360 * 5.0 / 16.0;
            } else if (signFace == BlockFace.NORTH_WEST) {
                yRotationDegree = 360 * 6.0 / 16.0;
            } else if (signFace == BlockFace.NORTH_NORTH_WEST) {
                yRotationDegree = 360 * 7.0 / 16.0;
            } else if (signFace == BlockFace.NORTH) {
                yRotationDegree = 360 * 8.0 / 16.0;
            } else if (signFace == BlockFace.NORTH_NORTH_EAST) {
                yRotationDegree = 360 * 9.0 / 16.0;
            } else if (signFace == BlockFace.NORTH_EAST) {
                yRotationDegree = 360 * 10.0 / 16.0;
            } else if (signFace == BlockFace.EAST_NORTH_EAST) {
                yRotationDegree = 360 * 11.0 / 16.0;
            } else if (signFace == BlockFace.EAST) {
                yRotationDegree = 360 * 12.0 / 16.0;
            } else if (signFace == BlockFace.EAST_SOUTH_EAST) {
                yRotationDegree = 360 * 13.0 / 16.0;
            } else if (signFace == BlockFace.SOUTH_EAST) {
                yRotationDegree = 360 * 14.0 / 16.0;
            } else if (signFace == BlockFace.SOUTH_SOUTH_EAST) {
                yRotationDegree = 360 * 15.0 / 16.0;
            }
        } else if (type.data == WallSign.class || type.data == WallHangingSign.class) {
            Directional directionalData = (Directional) data;
            signFace = directionalData.getFacing();
            if (signFace == BlockFace.SOUTH) {
                yRotationDegree = 0;
            } else if (signFace == BlockFace.WEST) {
                yRotationDegree = 90;
            } else if (signFace == BlockFace.NORTH) {
                yRotationDegree = 180;
            } else if (signFace == BlockFace.EAST) {
                yRotationDegree = 270;
            }
            // wall signs are not centered on the block (but hanging wall signs are)
            if (type.data == WallSign.class) {
                if (signFace == BlockFace.NORTH) {
                    centerz = 15.0 / 16.0;
                } else if (signFace == BlockFace.SOUTH) {
                    centerz = 1.0 / 16.0;
                } else if (signFace == BlockFace.WEST) {
                    centerx = 15.0 / 16.0;
                } else if (signFace == BlockFace.EAST) {
                    centerx = 1.0 / 16.0;
                }
            }
        } else {
            throw new IllegalArgumentException("block is not a sign");
        }

        Location entityLoc = entity.getLocation();
        double relativeX = entityLoc.getX() - (sign.getX() + centerx);
        double relativeZ = entityLoc.getZ() - (sign.getZ() + centerz);
        double f = Math.atan2(relativeZ, relativeX) * 180.0 / Math.PI - 90.0;

        return Math.abs(Utils.warpDegrees(f - yRotationDegree)) <= 90.0 ? Side.FRONT : Side.BACK;
    }
}
