package de.diddiz.LogBlock;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;

import static de.diddiz.util.ActionColor.DESTROY;
import static de.diddiz.util.MessagingUtil.*;

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
		if (date > 0) {
			msg.append(brackets(prettyDate(date), BracketType.STANDARD)).append(' ');
		}

		msg.append(killerName).append(DESTROY).append(" killed ").append(DEFAULT).append(victimName);
		if (loc != null) {
			msg.append(" at ").append(prettyLocation(loc));
		}
		String weaponName = prettyItemName(new ItemStack(weapon));
		msg.append(" with ").append(weaponName); // + ("aeiou".contains(weaponName.substring(0, 1)) ? "an " : "a " )
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
