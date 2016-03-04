package de.diddiz.LogBlock;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import static de.diddiz.util.BukkitUtils.entityName;
import org.bukkit.Bukkit;

public class Actor {

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + (this.UUID != null ? this.UUID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Actor other = (Actor) obj;
        return ((this.UUID == null && other.UUID == null) || this.UUID.equals(other.UUID));
    }

    final String name;
    final String UUID;

    public Actor(String name, String UUID) {
        this.name = name;
        this.UUID = UUID;

    }

    public Actor(String name, java.util.UUID UUID) {
        this.name = name;
        this.UUID = UUID.toString();

    }

    public Actor(String name) {
        this(name, generateUUID(name));
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

    public static Actor actorFromEntity(Entity entity) {
        if (entity instanceof Player) {
            return new Actor(entityName(entity), entity.getUniqueId());
        } else {
            return new Actor(entityName(entity));
        }
    }

    public static Actor actorFromEntity(EntityType entity) {
        return new Actor(entity.getName());
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
 * and if so, setting the UUID accordingly.  This only checks against currently online
 * players and is a "best effort" attempt for use with the pre-UUID API
 * <p>
 * If you know something is an entity (player or otherwise) use the {@link #actorFromEntity(org.bukkit.entity.Entity) }
 * or {@link #actorFromEntity(org.bukkit.entity.EntityType) } methods
 * <p>
 * If you know something is a server effect (like gravity) use {@link #Actor(java.lang.String)}
 * @deprecated Only use this if you have a String of unknown origin
 * 
 * @param actorName String of unknown origin
 * @return 
 */
    public static Actor actorFromString(String actorName) {
        Collection<? extends Player> players = Bukkit.getServer().getOnlinePlayers();
        for (Player p : players) {
            if (p.getName().equalsIgnoreCase(actorName)) {
                return actorFromEntity(p);
            }
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
