package de.diddiz.worldedit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockVector;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.util.nbt.BinaryTag;
import com.sk89q.worldedit.util.nbt.BinaryTagIO;
import com.sk89q.worldedit.util.nbt.CompoundBinaryTag;
import com.sk89q.worldedit.util.nbt.DoubleBinaryTag;
import com.sk89q.worldedit.util.nbt.ListBinaryTag;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.util.CuboidRegion;

public class WorldEditHelper {
    private static boolean checkedForWorldEdit;
    private static boolean hasWorldEdit;

    public static boolean hasWorldEdit() {
        if (!checkedForWorldEdit) {
            checkedForWorldEdit = true;
            Plugin worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit");
            hasWorldEdit = worldEdit != null;
            if (worldEdit != null) {
                Internal.setWorldEdit(worldEdit);
            }
        }
        return hasWorldEdit;
    }

    public static boolean hasFullWorldEdit() {
        return hasWorldEdit && Internal.hasBukkitImplAdapter();
    }

    public static byte[] serializeEntity(Entity entity) {
        if (!hasWorldEdit()) {
            return null;
        }
        return Internal.serializeEntity(entity);
    }

    public static Entity restoreEntity(Location location, EntityType type, byte[] serialized) {
        if (!hasWorldEdit()) {
            return null;
        }
        return Internal.restoreEntity(location, type, serialized);
    }

    public static CuboidRegion getSelectedRegion(Player player) throws IllegalArgumentException {
        if (!hasWorldEdit()) {
            throw new IllegalArgumentException("WorldEdit not found!");
        }
        return Internal.getSelectedRegion(player);
    }

    private static class Internal {
        private static WorldEditPlugin worldEdit;
        private static Method getBukkitImplAdapter;

        public static void setWorldEdit(Plugin worldEdit) {
            Internal.worldEdit = (WorldEditPlugin) worldEdit;
        }

        public static boolean hasBukkitImplAdapter() {
            if (getBukkitImplAdapter == null) {
                try {
                    getBukkitImplAdapter = WorldEditPlugin.class.getDeclaredMethod("getBukkitImplAdapter");
                    getBukkitImplAdapter.setAccessible(true);
                } catch (Exception e) {
                    LogBlock.getInstance().getLogger().log(Level.SEVERE, "Exception while checking for BukkitImplAdapter", e);
                    return false;
                }
            }
            try {
                return getBukkitImplAdapter.invoke(worldEdit) != null;
            } catch (Exception e) {
                LogBlock.getInstance().getLogger().log(Level.SEVERE, "Exception while checking for BukkitImplAdapter", e);
                return false;
            }
        }

        public static Entity restoreEntity(Location location, EntityType type, byte[] serialized) {
            com.sk89q.worldedit.world.entity.EntityType weType = BukkitAdapter.adapt(type);
            com.sk89q.worldedit.util.Location weLocation = BukkitAdapter.adapt(location);
            try {
                Entry<String, CompoundBinaryTag> namedTag = BinaryTagIO.unlimitedReader().readNamed(new ByteArrayInputStream(serialized));
                UUID newUUID = null;
                if (namedTag.getKey().equals("entity")) {
                    CompoundBinaryTag serializedState = namedTag.getValue();
                    BaseEntity state = new BaseEntity(weType, LazyReference.computed(serializedState));
                    com.sk89q.worldedit.entity.Entity weEntity = weLocation.getExtent().createEntity(weLocation, state);
                    if (weEntity != null) {
                        CompoundBinaryTag newNbt = weEntity.getState().getNbt();
                        int[] uuidInts = newNbt.getIntArray("UUID");
                        if (uuidInts != null && uuidInts.length >= 4) {
                            newUUID = new UUID(((long) uuidInts[0] << 32) | (uuidInts[1] & 0xFFFFFFFFL), ((long) uuidInts[2] << 32) | (uuidInts[3] & 0xFFFFFFFFL));
                        } else {
                            newUUID = new UUID(newNbt.getLong("UUIDMost"), newNbt.getLong("UUIDLeast")); // pre 1.16
                        }
                    }
                }
                return newUUID == null ? null : Bukkit.getEntity(newUUID);
            } catch (IOException e) {
                throw new RuntimeException("This IOException should be impossible", e);
            }
        }

        public static byte[] serializeEntity(Entity entity) {
            com.sk89q.worldedit.entity.Entity weEntity = BukkitAdapter.adapt(entity);
            BaseEntity state = weEntity.getState();
            if (state != null) {
                try {
                    CompoundBinaryTag nbt = state.getNbt();
                    nbt = nbt.putFloat("Health", 20.0f);
                    nbt = nbt.put("Motion", ListBinaryTag.from(Arrays.asList(new BinaryTag[] { DoubleBinaryTag.of(0.0), DoubleBinaryTag.of(0.0), DoubleBinaryTag.of(0.0) })));
                    nbt = nbt.putShort("Fire", (short) -20);
                    nbt = nbt.putShort("HurtTime", (short) 0);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    BinaryTagIO.writer().writeNamed(new AbstractMap.SimpleImmutableEntry<>("entity", nbt), baos);
                    return baos.toByteArray();
                } catch (IOException e) {
                    throw new RuntimeException("This IOException should be impossible", e);
                }
            }
            return null;
        }

        public static CuboidRegion getSelectedRegion(Player player) throws IllegalArgumentException {
            LocalSession session = worldEdit.getSession(player);
            World world = player.getWorld();
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
            if (!weWorld.equals(session.getSelectionWorld())) {
                throw new IllegalArgumentException("No selection defined");
            }
            Region selection;
            try {
                selection = session.getSelection(weWorld);
            } catch (IncompleteRegionException e) {
                throw new IllegalArgumentException("No selection defined");
            }
            if (selection == null) {
                throw new IllegalArgumentException("No selection defined");
            }
            if (!(selection instanceof com.sk89q.worldedit.regions.CuboidRegion)) {
                throw new IllegalArgumentException("You have to define a cuboid selection");
            }
            BlockVector3 min = selection.getMinimumPoint();
            BlockVector3 max = selection.getMaximumPoint();
            return new CuboidRegion(world, new BlockVector(min.getBlockX(), min.getBlockY(), min.getBlockZ()), new BlockVector(max.getBlockX(), max.getBlockY(), max.getBlockZ()));
        }
    }
}
