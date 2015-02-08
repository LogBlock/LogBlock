package de.diddiz.LogBlock;

import static de.diddiz.util.MaterialName.materialName;
import java.sql.ResultSet;
import java.sql.SQLException;

import de.diddiz.util.BukkitUtils;
import org.bukkit.Location;

import de.diddiz.LogBlock.config.Config;
import org.bukkit.Material;

public class BlockChange extends AbstractLookupCacheElement
{
	public final long id, date;
	public final Location loc;
	public final String playerName;
	public final int replaced, type;
	public final byte data;
	public final String signtext;
	public final ChestAccess ca;

	public BlockChange(long date, Location loc, String playerName, int replaced, int type, byte data, String signtext, ChestAccess ca) {
		id = 0;
		this.date = date;
		this.loc = loc;
		this.playerName = playerName;
		this.replaced = replaced;
		this.type = type;
		this.data = data;
		this.signtext = signtext;
		this.ca = ca;
	}

	public BlockChange(ResultSet rs, QueryParams p) throws SQLException {
		id = p.needId ? rs.getInt("id") : 0;
		date = p.needDate ? rs.getTimestamp("date").getTime() : 0;
		loc = p.needCoords ? new Location(p.world, rs.getInt("x"), rs.getInt("y"), rs.getInt("z")) : null;
		playerName = p.needPlayer ? rs.getString("playername") : null;
		replaced = p.needType ? rs.getInt("replaced") : 0;
		type = p.needType ? rs.getInt("type") : 0;
		data = p.needData ? rs.getByte("data") : (byte)0;
		signtext = p.needSignText ? rs.getString("signtext") : null;
		ca = p.needChestAccess && rs.getShort("itemtype") != 0 && rs.getShort("itemamount") != 0 ? new ChestAccess(rs.getShort("itemtype"), rs.getShort("itemamount"), rs.getShort("itemdata")) : null;
	}

	@Override
	public String toString() {
		final StringBuilder msg = new StringBuilder();
		if (date > 0)
			msg.append(Config.formatter.format(date)).append(" ");
		if (playerName != null)
			msg.append(playerName).append(" ");
		if (signtext != null) {
			final String action = type == 0 ? "destroyed " : "created ";
			if (!signtext.contains("\0"))
				msg.append(action).append(signtext);
			else
				msg.append(action).append(materialName(type != 0 ? type : replaced)).append(" [").append(signtext.replace("\0", "] [")).append("]");
		} else if (type == replaced) {
			if (type == 0)
				msg.append("did an unspecified action");
			else if (ca != null) {
				if (ca.itemType == 0 || ca.itemAmount == 0)
					msg.append("looked inside ").append(materialName(type));
				else if (ca.itemAmount < 0)
					msg.append("took ").append(-ca.itemAmount).append("x ").append(materialName(ca.itemType, ca.itemData)).append(" from ").append(materialName(type));
				else
					msg.append("put ").append(ca.itemAmount).append("x ").append(materialName(ca.itemType, ca.itemData)).append(" into ").append(materialName(type));
			} else if (BukkitUtils.getContainerBlocks().contains(Material.getMaterial(type)))
				msg.append("opened ").append(materialName(type));
			else if (type == 64 || type == 71)
				// This is a problem that will have to be addressed in LB 2,
				// there is no way to tell from the top half of the block if
				// the door is opened or closed.
				msg.append("moved ").append(materialName(type));
			// Trapdoor
			else if (type == 96)
				msg.append((data < 8 || data > 11) ? "opened" : "closed").append(" ").append(materialName(type));
			// Fence gate
			else if (type == 107)
				msg.append(data > 3 ? "opened" : "closed").append(" ").append(materialName(type));
			else if (type == 69)
				msg.append("switched ").append(materialName(type));
			else if (type == 77 || type == 143)
				msg.append("pressed ").append(materialName(type));
			else if (type == 92)
				msg.append("ate a piece of ").append(materialName(type));
			else if (type == 25 || type == 93 || type == 94 || type == 149 || type == 150)
				msg.append("changed ").append(materialName(type));
			else if (type == 70 || type == 72 || type == 147 || type == 148)
				msg.append("stepped on ").append(materialName(type));
			else if (type == 132)
				msg.append("ran into ").append(materialName(type));
		} else if (type == 0)
			msg.append("destroyed ").append(materialName(replaced, data));
		else if (replaced == 0)
			msg.append("created ").append(materialName(type, data));
		else
			msg.append("replaced ").append(materialName(replaced, (byte)0)).append(" with ").append(materialName(type, data));
		if (loc != null)
			msg.append(" at ").append(loc.getBlockX()).append(":").append(loc.getBlockY()).append(":").append(loc.getBlockZ());
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
}
