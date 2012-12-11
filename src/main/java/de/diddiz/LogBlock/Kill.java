package de.diddiz.LogBlock;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import de.diddiz.LogBlock.config.Config;

public class Kill implements LookupCacheElement
{
	final long id, date;
	public final Location loc;
	final String killerName, victimName;
	final int weapon;

	public Kill(String killerName, String victimName, int weapon, Location loc) {
		id = 0;
		date = System.currentTimeMillis() / 1000;
		this.loc = loc;
		this.killerName = killerName;
		this.victimName = victimName;
		this.weapon = weapon;
	}

	public Kill(ResultSet rs, QueryParams p) throws SQLException {
		id = p.needId ? rs.getInt("id") : 0;
		date = p.needDate ? rs.getTimestamp("date").getTime() : 0;
		loc = p.needCoords ? new Location(p.world, rs.getInt("x"), rs.getInt("y"), rs.getInt("z")) : null;
		killerName = p.needKiller ? rs.getString("killer") : null;
		victimName = p.needVictim ? rs.getString("victim") : null;
		weapon = p.needWeapon ? rs.getInt("weapon") : 0;
	}

	@Override
	public String toString() {
		final StringBuilder msg = new StringBuilder();
		if (date > 0)
			msg.append(Config.formatter.format(date)).append(" ");
		msg.append(killerName).append(" killed ").append(victimName);
		if (loc != null)
			msg.append(" at ").append(loc.getBlockX()).append(":").append(loc.getBlockY()).append(":").append(loc.getBlockZ());
		String weaponName = prettyItemName(new ItemStack(weapon));
		msg.append(" with " + weaponName); // + ("aeiou".contains(weaponName.substring(0, 1)) ? "an " : "a " )
		return msg.toString();
	}

	@Override
	public Location getLocation() {
		return loc;
	}

	@Override
	public String getMessage() {
		return toString();
	}

	public String prettyItemName(ItemStack i) {
		String item = i.getType().toString().replace('_', ' ' ).toLowerCase();
		if(item.equals("air")) {
			item = "fist";
		}
		return item;
	}
}
