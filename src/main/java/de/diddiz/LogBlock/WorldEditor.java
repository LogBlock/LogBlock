package de.diddiz.LogBlock;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Bed.Part;
import org.bukkit.block.data.type.Piston;
import org.bukkit.block.data.type.PistonHead;
import org.bukkit.block.data.type.TechnicalPiston.Type;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import de.diddiz.LogBlock.QueryParams.Order;
import de.diddiz.LogBlock.blockstate.BlockStateCodecs;
import de.diddiz.LogBlock.util.BukkitUtils;
import de.diddiz.LogBlock.util.Utils;
import de.diddiz.LogBlock.worldedit.WorldEditHelper;
import java.io.File;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

import static de.diddiz.LogBlock.config.Config.dontRollback;
import static de.diddiz.LogBlock.config.Config.replaceAnyway;
import static de.diddiz.LogBlock.util.BukkitUtils.*;

public class WorldEditor implements Runnable {
    private final LogBlock logblock;
    private final ArrayList<Edit> edits = new ArrayList<>();
    private int rowsCompleted;
    private int totalRows;
    private final World world;

    /**
     * The player responsible for editing the world, used to report progress
     */
    private CommandSender sender;
    private int taskID;
    private int successes = 0, blacklistCollisions = 0;
    private long elapsedTime = 0;
    public LookupCacheElement[] errors;
    private boolean forceReplace;
    private HashMap<Integer, UUID> uuidReplacements = new HashMap<>();
    private boolean started = false;

    public WorldEditor(LogBlock logblock, World world) {
        this(logblock, world, false);
    }

    public WorldEditor(LogBlock logblock, World world, boolean forceReplace) {
        this.logblock = logblock;
        this.world = world;
        this.forceReplace = forceReplace;
    }

    public int getSize() {
        return edits.size();
    }

    public int getSuccesses() {
        return successes;
    }

    public int getErrors() {
        return errors.length;
    }

    public int getBlacklistCollisions() {
        return blacklistCollisions;
    }

    public void setSender(CommandSender sender) {
        this.sender = sender;
    }

    public void queueBlockEdit(long time, int x, int y, int z, int replaced, int replaceData, byte[] replacedState, int type, int typeData, byte[] typeState, ChestAccess item) {
        if (started) {
            throw new IllegalStateException("Already started");
        }
        edits.add(new BlockEdit(time, new Location(world, x, y, z), null, replaced, replaceData, replacedState, type, typeData, typeState, item));
    }

    public void queueEntityEdit(ResultSet rs, QueryParams p, boolean rollback) throws SQLException {
        if (started) {
            throw new IllegalStateException("Already started");
        }
        edits.add(new EntityEdit(rs, p, rollback));
    }

    public void reverseRowOrder() {
        if (started) {
            throw new IllegalStateException("Already started");
        }
        Collections.reverse(edits);
    }

    public void sortRows(QueryParams.Order order) {
        if (started) {
            throw new IllegalStateException("Already started");
        }
        edits.sort(new EditComparator(order));
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    synchronized public void start() throws Exception {
        if (started) {
            throw new IllegalStateException("Already started");
        }
        started = true;
        final long start = System.currentTimeMillis();
        totalRows = edits.size();
        taskID = logblock.getServer().getScheduler().scheduleSyncRepeatingTask(logblock, this, 0, 1);
        if (taskID == -1) {
            throw new Exception("Failed to schedule task");
        }
        try {
            this.wait();
        } catch (final InterruptedException ex) {
            throw new Exception("Interrupted");
        }
        elapsedTime = System.currentTimeMillis() - start;
    }

    @Override
    public synchronized void run() {
        final List<WorldEditorException> errorList = new ArrayList<>();
        int counter = 0;
        long t0 = System.nanoTime();
        long maxEditTime = 5_000_000; // 5 ms
        while (!edits.isEmpty() && counter < 10000 && (counter < 100 || counter % 10 != 0 || System.nanoTime() - t0 < maxEditTime)) {
            try {
                switch (edits.remove(edits.size() - 1).perform()) {
                    case SUCCESS:
                        successes++;
                        break;
                    case BLACKLISTED:
                        blacklistCollisions++;
                        break;
                    case NO_ACTION:
                        break;
                }
            } catch (final WorldEditorException ex) {
                errorList.add(ex);
            } catch (final Exception ex) {
                logblock.getLogger().log(Level.WARNING, "[WorldEditor] Exeption: ", ex);
            }
            rowsCompleted++;
            counter++;
            if (sender != null) {
                float percentage = rowsCompleted * 100.0f / totalRows;
                if (rowsCompleted % 10000 == 0) {
                    sender.sendMessage(ChatColor.GOLD + "[LogBlock]" + ChatColor.YELLOW + " Rollback progress: " + NumberFormat.getNumberInstance().format(percentage) + "%" +
                            " Blocks edited: " + counter);
                }
            }
        }
        if (edits.isEmpty()) {
            logblock.getServer().getScheduler().cancelTask(taskID);
            if (errorList.size() > 0) {
                try {
                    final File errorDir = new File(logblock.getDataFolder(), "error");
                    errorDir.mkdirs();
                    final File file = new File(errorDir, "WorldEditor-" + new SimpleDateFormat("yy-MM-dd-HH-mm-ss").format(System.currentTimeMillis()) + ".log");
                    final PrintWriter writer = new PrintWriter(file);
                    for (final WorldEditorException err : errorList) {
                        writer.println(BaseComponent.toPlainText(err.getLogMessage()));
                        err.printStackTrace(writer);
                        writer.println();
                        writer.println();
                    }
                    writer.close();
                } catch (final Exception ex) {
                }
            }
            errors = errorList.toArray(new WorldEditorException[errorList.size()]);
            notify();
        }
    }

    protected UUID getReplacedUUID(int entityid, UUID unreplaced) {
        UUID replaced = uuidReplacements.get(entityid);
        return replaced != null ? replaced : unreplaced;
    }

    public static enum PerformResult {
        SUCCESS,
        BLACKLISTED,
        NO_ACTION
    }

    public interface Edit {
        PerformResult perform() throws WorldEditorException;

        public long getTime();
    }

    public class EntityEdit extends EntityChange implements Edit {
        private boolean rollback;

        public EntityEdit(ResultSet rs, QueryParams p, boolean rollback) throws SQLException {
            super(rs, p);
            this.rollback = rollback;
        }

        @Override
        public long getTime() {
            return date;
        }

        @Override
        public PerformResult perform() throws WorldEditorException {
            if (type == null) {
                throw new WorldEditorException("Unkown entity type for entity " + entityUUID, loc);
            }
            if (changeType == (rollback ? EntityChangeType.KILL : EntityChangeType.CREATE)) {
                // spawn entity
                UUID uuid = getReplacedUUID(entityId, entityUUID);
                Entity result = null;
                YamlConfiguration deserialized = Utils.deserializeYamlConfiguration(data);
                double x = deserialized.getDouble("x");
                double y = deserialized.getDouble("y");
                double z = deserialized.getDouble("z");
                float yaw = (float) deserialized.getDouble("yaw");
                float pitch = (float) deserialized.getDouble("pitch");
                Location location = new Location(world, x, y, z, yaw, pitch);
                Entity existing = BukkitUtils.loadEntityAround(location.getChunk(), uuid);
                if (existing != null) {
                    return PerformResult.NO_ACTION;
                }
                byte[] serializedWorldEditEntity = (byte[]) deserialized.get("worldedit");
                if (serializedWorldEditEntity != null) {
                    result = WorldEditHelper.restoreEntity(location, type, serializedWorldEditEntity);
                }
                if (result == null) {
                    throw new WorldEditorException("Could not restore " + type, location);
                } else {
                    if (!result.getUniqueId().equals(uuid)) {
                        logblock.getConsumer().queueEntityUUIDChange(world, entityId, result.getUniqueId());
                        uuidReplacements.put(entityId, result.getUniqueId());
                    }
                }
                return PerformResult.SUCCESS;
            } else if (changeType == (rollback ? EntityChangeType.CREATE : EntityChangeType.KILL)) {
                // kill entity
                UUID uuid = getReplacedUUID(entityId, entityUUID);
                YamlConfiguration deserialized = Utils.deserializeYamlConfiguration(data);
                double x = deserialized.getDouble("x");
                double y = deserialized.getDouble("y");
                double z = deserialized.getDouble("z");
                float yaw = (float) deserialized.getDouble("yaw");
                float pitch = (float) deserialized.getDouble("pitch");
                Location location = new Location(world, x, y, z, yaw, pitch);
                Entity existing = BukkitUtils.loadEntityAround(location.getChunk(), uuid);
                if (existing != null) {
                    existing.remove();
                    return PerformResult.SUCCESS;
                }
                return PerformResult.NO_ACTION; // the entity is not there, so we cannot do anything
            } else if (changeType == (rollback ? EntityChangeType.REMOVEEQUIP : EntityChangeType.ADDEQUIP)) {
                // set equip
                UUID uuid = getReplacedUUID(entityId, entityUUID);
                Entity existing = BukkitUtils.loadEntityAround(loc.getChunk(), uuid);
                if (existing != null) {
                    YamlConfiguration deserialized = Utils.deserializeYamlConfiguration(data);
                    ItemStack item = deserialized.getItemStack("item");
                    if (item != null && existing instanceof ItemFrame) {
                        ItemStack old = ((ItemFrame) existing).getItem();
                        if (old == null || old.getType() == Material.AIR) {
                            ((ItemFrame) existing).setItem(item);
                            return PerformResult.SUCCESS;
                        }
                    } else if (item != null && existing instanceof ArmorStand) {
                        EquipmentSlot slot = EquipmentSlot.valueOf(deserialized.getString("slot"));
                        ArmorStand stand = (ArmorStand) existing;
                        ItemStack old = BukkitUtils.getItemInSlot(stand, slot);
                        if (old == null || old.getType() == Material.AIR) {
                            BukkitUtils.setItemInSlot(stand, slot, item);
                            return PerformResult.SUCCESS;
                        }
                    }
                }
                return PerformResult.NO_ACTION; // the entity is not there, or equip does not match
            } else if (changeType == (rollback ? EntityChangeType.ADDEQUIP : EntityChangeType.REMOVEEQUIP)) {
                // remove equip
                UUID uuid = getReplacedUUID(entityId, entityUUID);
                Entity existing = BukkitUtils.loadEntityAround(loc.getChunk(), uuid);
                if (existing != null) {
                    YamlConfiguration deserialized = Utils.deserializeYamlConfiguration(data);
                    ItemStack item = deserialized.getItemStack("item");
                    if (item != null && existing instanceof ItemFrame) {
                        ItemStack old = ((ItemFrame) existing).getItem();
                        if (old != null && old.isSimilar(item)) {
                            ((ItemFrame) existing).setItem(null);
                            return PerformResult.SUCCESS;
                        }
                    } else if (item != null && existing instanceof ArmorStand) {
                        EquipmentSlot slot = EquipmentSlot.valueOf(deserialized.getString("slot"));
                        ArmorStand stand = (ArmorStand) existing;
                        ItemStack old = BukkitUtils.getItemInSlot(stand, slot);
                        if (old != null && old.isSimilar(item)) {
                            BukkitUtils.setItemInSlot(stand, slot, null);
                            return PerformResult.SUCCESS;
                        }
                    }
                }
                return PerformResult.NO_ACTION; // the entity is not there, or equip does not match
            } else if (changeType == EntityChangeType.GET_STUNG) {
                UUID uuid = getReplacedUUID(entityId, entityUUID);
                Entity existing = BukkitUtils.loadEntityAround(loc.getChunk(), uuid);
                if (existing != null && existing instanceof Bee) {
                    ((Bee) existing).setHasStung(!rollback);
                }
            }
            return PerformResult.NO_ACTION;
        }
    }

    public class BlockEdit extends BlockChange implements Edit {
        public BlockEdit(long time, Location loc, Actor actor, int replaced, int replaceData, byte[] replacedState, int type, int typeData, byte[] typeState, ChestAccess ca) {
            super(time, loc, actor, replaced, replaceData, replacedState, type, typeData, typeState, ca);
        }

        @Override
        public long getTime() {
            return date;
        }

        @Override
        public PerformResult perform() throws WorldEditorException {
            BlockData replacedBlock = getBlockReplaced();
            BlockData setBlock = getBlockSet();
            if (replacedBlock == null || setBlock == null) {
                throw new WorldEditorException("Could not parse the material", loc.clone());
            }
            // action: set to replaced

            if (dontRollback.contains(replacedBlock.getMaterial())) {
                return PerformResult.BLACKLISTED;
            }
            final Block block = loc.getBlock();
            if (BukkitUtils.isEmpty(replacedBlock.getMaterial()) && BukkitUtils.isEmpty(block.getType())) {
                return PerformResult.NO_ACTION;
            }
            BlockState state = block.getState();
            if (setBlock.equals(replacedBlock)) {
                if (ca != null) {
                    if (state instanceof InventoryHolder && state.getType() == replacedBlock.getMaterial()) {
                        int leftover;
                        try {
                            leftover = modifyContainer(state, ca.itemStack, !ca.remove);
                        } catch (final Exception ex) {
                            throw new WorldEditorException(ex.getMessage(), block.getLocation());
                        }
                        if (leftover > 0 && ca.remove) {
                            throw new WorldEditorException("Not enough space left in " + block.getType(), block.getLocation());
                        }
                        return PerformResult.SUCCESS;
                    }
                    return PerformResult.NO_ACTION;
                }
            }
            if (!forceReplace && !BukkitUtils.isSimilarForRollback(setBlock.getMaterial(), block.getType()) && !block.isEmpty() && !replaceAnyway.contains(block.getType())) {
                return PerformResult.NO_ACTION;
            }
            if (state instanceof Container && replacedBlock.getMaterial() != block.getType()) {
                ((Container) state).getSnapshotInventory().clear();
                state.update();
            }
            block.setBlockData(replacedBlock);
            BlockData newData = block.getBlockData();
            if (BlockStateCodecs.hasCodec(replacedBlock.getMaterial())) {
                state = block.getState();
                try {
                    BlockStateCodecs.deserialize(state, Utils.deserializeYamlConfiguration(replacedState));
                    state.update();
                } catch (Exception e) {
                    throw new WorldEditorException("Failed to restore blockstate of " + block.getType() + ": " + e, block.getLocation());
                }
            }

            final Material curtype = block.getType();
            if (newData instanceof Bed) {
                final Bed bed = (Bed) newData;
                final Block secBlock = bed.getPart() == Part.HEAD ? block.getRelative(bed.getFacing().getOppositeFace()) : block.getRelative(bed.getFacing());
                if (secBlock.isEmpty()) {
                    Bed bed2 = (Bed) bed.clone();
                    bed2.setPart(bed.getPart() == Part.HEAD ? Part.FOOT : Part.HEAD);
                    secBlock.setBlockData(bed2);
                }
            } else if (curtype == Material.IRON_DOOR || BukkitUtils.isWoodenDoor(curtype) || BukkitUtils.isDoublePlant(curtype)) {
                final Bisected firstPart = (Bisected) newData;
                final Block secBlock = block.getRelative(firstPart.getHalf() == Half.TOP ? BlockFace.DOWN : BlockFace.UP);
                if (secBlock.isEmpty()) {
                    Bisected secondPart = (Bisected) firstPart.clone();
                    secondPart.setHalf(firstPart.getHalf() == Half.TOP ? Half.BOTTOM : Half.TOP);
                    secBlock.setBlockData(secondPart);
                }
            } else if ((curtype == Material.PISTON || curtype == Material.STICKY_PISTON)) {
                Piston piston = (Piston) newData;
                if (piston.isExtended()) {
                    final Block secBlock = block.getRelative(piston.getFacing());
                    if (secBlock.isEmpty()) {
                        PistonHead head = (PistonHead) Material.PISTON_HEAD.createBlockData();
                        head.setFacing(piston.getFacing());
                        head.setType(curtype == Material.PISTON ? Type.NORMAL : Type.STICKY);
                        secBlock.setBlockData(head);
                    }
                }
            } else if (curtype == Material.PISTON_HEAD) {
                PistonHead head = (PistonHead) newData;
                final Block secBlock = block.getRelative(head.getFacing().getOppositeFace());
                if (secBlock.isEmpty()) {
                    Piston piston = (Piston) (head.getType() == Type.NORMAL ? Material.PISTON : Material.STICKY_PISTON).createBlockData();
                    piston.setFacing(head.getFacing());
                    piston.setExtended(true);
                    secBlock.setBlockData(piston);
                }
            } else if (newData instanceof Chest) {
                Chest chest = (Chest) newData;
                if (chest.getType() != org.bukkit.block.data.type.Chest.Type.SINGLE) {
                    if (getConnectedChest(block) == null) {
                        chest.setType(org.bukkit.block.data.type.Chest.Type.SINGLE);
                        block.setBlockData(chest);
                    }
                }
            }
            return PerformResult.SUCCESS;
        }
    }

    public static class EditComparator implements Comparator<Edit> {
        private final int mult;

        public EditComparator(QueryParams.Order order) {
            mult = order == Order.DESC ? 1 : -1;
        }

        @Override
        public int compare(Edit edit1, Edit edit2) {
            long time1 = edit1.getTime();
            long time2 = edit2.getTime();
            return time1 > time2 ? mult : time1 < time2 ? -mult : 0;
        }
    }

    @SuppressWarnings("serial")
    public static class WorldEditorException extends Exception implements LookupCacheElement {
        private final Location loc;

        public WorldEditorException(Material typeBefore, Material typeAfter, Location loc) {
            this("Failed to replace " + typeBefore.name() + " with " + typeAfter.name(), loc);
        }

        public WorldEditorException(String msg, Location loc) {
            super(msg + " at " + loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ());
            this.loc = loc;
        }

        @Override
        public Location getLocation() {
            return loc;
        }

        @Override
        public BaseComponent getLogMessage(int entry) {
            return TextComponent.fromLegacy(getMessage());
        }
    }
}
