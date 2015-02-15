package de.diddiz.LogBlock;

import static de.diddiz.util.BukkitUtils.entityName;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.projectiles.ProjectileSource;

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
		if ((this.UUID == null) ? (other.UUID != null) : !this.UUID.equals(other.UUID)) {
			return false;
		}
		return true;
	}

	final String name;
	final String UUID;

	public Actor(String name, String UUID) {
		this.name = name;
		this.UUID = UUID;
		
	}

	public Actor(String name) {
		this(name, generateUUID(name));
	}
	
	public Actor(ResultSet rs) throws SQLException {
		this(rs.getString("playername"),rs.getString("UUID"));
	}
	
	public String getName() {
		return name;
	}

	public String getUUID() {
		return UUID;
	}

	public static Actor actorFromEntity(Entity entity) {
		if (entity instanceof Player) {
			return new Actor(entityName(entity),entity.getUniqueId().toString());
		} else {
			return new Actor(entityName(entity));
		}
	}
	
	public static Actor actorFromEntity(EntityType entity) {
		return new Actor(entity.getName());
	}


	public static Actor actorFromProjectileSource(ProjectileSource psource) {
			if (psource instanceof Player) {
				Player player = ((Player) psource).getPlayer();
			return new Actor(player.getName(),player.getUniqueId().toString());
		} else {
			return new Actor(psource.toString());
		}
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
