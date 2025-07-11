package de.diddiz.LogBlock;

import static de.diddiz.LogBlock.util.ActionColor.CREATE;
import static de.diddiz.LogBlock.util.ActionColor.DESTROY;
import static de.diddiz.LogBlock.util.ActionColor.INTERACT;
import static de.diddiz.LogBlock.util.MessagingUtil.createTextComponentWithColor;
import static de.diddiz.LogBlock.util.MessagingUtil.prettyDate;
import static de.diddiz.LogBlock.util.MessagingUtil.prettyEntityType;
import static de.diddiz.LogBlock.util.MessagingUtil.prettyLocation;
import static de.diddiz.LogBlock.util.MessagingUtil.prettyMaterial;

import de.diddiz.LogBlock.componentwrapper.Component;
import de.diddiz.LogBlock.componentwrapper.Components;
import de.diddiz.LogBlock.util.Utils;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public class EntityChange implements LookupCacheElement {
    public static enum EntityChangeType {
        CREATE,
        KILL,
        MODIFY,
        ADDEQUIP,
        REMOVEEQUIP,
        GET_STUNG;

        private static EntityChangeType[] values = values();

        public static EntityChangeType valueOf(int ordinal) {
            return values[ordinal];
        }
    }

    public final long id, date;
    public final Location loc;
    public final Actor actor;
    public final EntityType type;
    public final int entityId;
    public final UUID entityUUID;
    public final EntityChangeType changeType;
    public final byte[] data;

    public EntityChange(long date, Location loc, Actor actor, EntityType type, UUID entityid, EntityChangeType changeType, byte[] data) {
        id = 0;
        this.date = date;
        this.loc = loc;
        this.actor = actor;
        this.type = type;
        this.entityId = -1;
        this.entityUUID = entityid;
        this.changeType = changeType;
        this.data = data;
    }

    public EntityChange(ResultSet rs, QueryParams p) throws SQLException {
        id = p.needId ? rs.getLong("id") : 0;
        date = p.needDate ? rs.getTimestamp("date").getTime() : 0;
        loc = p.needCoords ? new Location(p.world, rs.getInt("x"), rs.getInt("y"), rs.getInt("z")) : null;
        actor = p.needPlayer ? new Actor(rs) : null;
        type = p.needType ? EntityTypeConverter.getEntityType(rs.getInt("entitytypeid")) : null;
        entityId = p.needData ? rs.getInt("entityid") : 0;
        entityUUID = p.needData ? UUID.fromString(rs.getString("entityuuid")) : null;
        changeType = p.needType ? EntityChangeType.valueOf(rs.getInt("action")) : null;
        data = p.needData ? rs.getBytes("data") : null;
    }

    @Override
    public String toString() {
        return Components.toPlainText(getLogMessage());
    }

    @Override
    public Component getLogMessage(int entry) {
        Component msg = Components.empty();
        if (date > 0) {
            msg = msg.append(prettyDate(date));
            msg = msg.append(" ");
        }
        if (actor != null) {
            msg = msg.append(actor.getName());
            msg = msg.append(" ");
        }
        if (changeType == EntityChangeType.CREATE) {
            msg = msg.append(createTextComponentWithColor("created ", CREATE.getColor()));
        } else if (changeType == EntityChangeType.KILL) {
            boolean living = type != null && LivingEntity.class.isAssignableFrom(type.getEntityClass()) && !ArmorStand.class.isAssignableFrom(type.getDeclaringClass());
            msg = msg.append(createTextComponentWithColor(living ? "killed " : "destroyed ", DESTROY.getColor()));
        } else if (changeType == EntityChangeType.ADDEQUIP) {
            YamlConfiguration conf = Utils.deserializeYamlConfiguration(data);
            ItemStack stack = conf == null ? null : conf.getItemStack("item");
            if (stack == null) {
                msg = msg.append(createTextComponentWithColor("added an item to ", CREATE.getColor()));
            } else {
                msg = msg.append(createTextComponentWithColor("added ", CREATE.getColor()));
                msg = msg.append(prettyMaterial(stack.getType()));
                msg = msg.append(" to ");
            }
        } else if (changeType == EntityChangeType.REMOVEEQUIP) {
            YamlConfiguration conf = Utils.deserializeYamlConfiguration(data);
            ItemStack stack = conf == null ? null : conf.getItemStack("item");
            if (stack == null) {
                msg = msg.append(createTextComponentWithColor("removed an item from ", DESTROY.getColor()));
            } else {
                msg = msg.append(createTextComponentWithColor("removed ", DESTROY.getColor()));
                msg = msg.append(prettyMaterial(stack.getType()));
                msg = msg.append(" from ");
            }
        } else if (changeType == EntityChangeType.MODIFY) {
            msg = msg.append(createTextComponentWithColor("modified ", INTERACT.getColor()));
        } else if (changeType == EntityChangeType.GET_STUNG) {
            msg = msg.append(createTextComponentWithColor("got stung by ", DESTROY.getColor()));
        } else {
            msg = msg.append(createTextComponentWithColor("did an unknown action to ", INTERACT.getColor()));
        }
        if (type != null) {
            msg = msg.append(prettyEntityType(type));
        } else {
            msg = msg.append(prettyMaterial("an unknown entity"));
        }
        if (loc != null) {
            msg = msg.append(" at ");
            msg = msg.append(prettyLocation(loc, entry));
        }
        return msg;
    }

    @Override
    public Location getLocation() {
        return loc;
    }
}
