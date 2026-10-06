package de.diddiz.LogBlock;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;

import static de.diddiz.LogBlock.util.BukkitUtils.entityName;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class Actor {

    @Override
    public int hashCode() {
        return this.UUID != null ? this.UUID.hashCode() : 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Actor other = (Actor) obj;
        return (this.UUID == null) ? (other.UUID == null) : this.UUID.equals(other.UUID);
    }

    final String name;
    final String UUID;
    final Location blockLocation;
    final Entity entity;

    public Actor(String name, String UUID) {
        this.name = name;
        this.UUID = UUID == null ? "unknown" : (UUID.length() > 36 ? UUID.substring(0, 36) : UUID);
        this.blockLocation = null;
        this.entity = null;
    }

    public Actor(String name, String UUID, Block block) {
        this.name = name;
        this.UUID = UUID == null ? "unknown" : (UUID.length() > 36 ? UUID.substring(0, 36) : UUID);
        this.blockLocation = block == null ? null : block.getLocation();
        this.entity = null;
    }

    public Actor(String name, java.util.UUID UUID) {
        this.name = name;
        this.UUID = UUID.toString();
        this.blockLocation = null;
        this.entity = null;
    }

    public Actor(String name, java.util.UUID UUID, Block block) {
        this.name = name;
        this.UUID = UUID.toString();
        this.blockLocation = block == null ? null : block.getLocation();
        this.entity = null;
    }

    public Actor(String name, java.util.UUID UUID, Entity entity) {
        this.name = name;
        this.UUID = UUID.toString();
        this.blockLocation = null;
        this.entity = entity;
    }

    public Actor(String name) {
        this(name, generateUUID(name));
    }

    public Actor(String name, Block block) {
        this(name, generateUUID(name), block);
    }

    public Actor(String name, Entity entity) {
        this.name = name;
        this.UUID = generateUUID(name);
        this.blockLocation = null;
        this.entity = entity;
    }

    public Actor(ResultSet rs) throws SQLException {
        this(rs.getString("playername"), rs.getString("UUID"));
    }

    public String getName() {
        return name;
    }

    public String getUUID() {
        return UUID;
    }

    public Location getBlockLocation() {
        return blockLocation;
    }

    /**
     * The acting entity object (if known)
     */
    public Entity getEntity() {
        return entity;
    }

    public static Actor actorFromEntity(Entity entity) {
        if (entity instanceof Player) {
            return new Actor(entityName(entity), entity.getUniqueId(), entity);
        }
        if (entity instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) entity).getShooter();
            if (shooter != null) {
                return actorFromProjectileSource(shooter);
            }
        }
        return new Actor(entityName(entity), entity);
    }

    @Deprecated
    public static Actor actorFromEntity(EntityType entity) {
        return new Actor(entity.name());
    }

    public static Actor actorFromProjectileSource(ProjectileSource psource) {
        if (psource instanceof Entity) {
            return actorFromEntity((Entity) psource);
        }
        if (psource instanceof BlockProjectileSource) {
            return new Actor(((BlockProjectileSource) psource).getBlock().getType().toString());
        } else {
            return new Actor(psource.toString());
        }

    }

    /**
     * Generate an Actor object from a String name, trying to guess if it's an online player
     * and if so, setting the UUID accordingly. This only checks against currently online
     * players and is a "best effort" attempt for use with the pre-UUID API
     * <p>
     * If you know something is an entity (player or otherwise) use the {@link #actorFromEntity(org.bukkit.entity.Entity) }
     * or {@link #actorFromEntity(org.bukkit.entity.EntityType) } methods
     * <p>
     * If you know something is a server effect (like gravity) use {@link #Actor(java.lang.String)}
     *
     * @deprecated Only use this if you have a String of unknown origin
     *
     * @param actorName
     *            String of unknown origin
     * @return
     */
    @Deprecated
    public static Actor actorFromString(String actorName) {
        Player p = Bukkit.getServer().getPlayerExact(actorName);
        if (p != null) {
            return actorFromEntity(p);
        }
        // No player found online with that name, assuming non-player entity/effect
        return new Actor(actorName);
    }

    public static boolean isValidUUID(String uuid) {
        try {
            java.util.UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String generateUUID(String name) {
        return "log_" + name;

    }

}
