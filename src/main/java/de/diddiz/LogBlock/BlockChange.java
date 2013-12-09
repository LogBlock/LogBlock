package de.diddiz.LogBlock;

import static de.diddiz.util.MaterialName.materialName;

import java.sql.ResultSet;
import java.sql.SQLException;

import de.diddiz.util.BukkitUtils;
import de.diddiz.util.serializable.itemstack.SerializableItemStack;
import de.diddiz.util.serializable.util.ConversionUtil;
import org.bukkit.Location;

import de.diddiz.LogBlock.config.Config;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class BlockChange implements LookupCacheElement
{
	public final long id, date;
	public final Location loc;
	public final String playerName;
	public final int beforeID, afterID;
	public final byte data;
	public final String signtext;
	public final SerializableItemStack itemStack;

	public BlockChange(long date, Location loc, String playerName, int beforeID, int afterID, byte data, String signtext, SerializableItemStack itemStack) {
		id = 0;
		this.date = date;
		this.loc = loc;
		this.playerName = playerName;
		this.beforeID = beforeID;
		this.afterID = afterID;
		this.data = data;
		this.signtext = signtext;
		this.itemStack = itemStack;
	}

	public BlockChange(ResultSet rs, QueryParams p) throws SQLException {
		id = p.needId ? rs.getInt("id") : 0;
		date = p.needDate ? rs.getTimestamp("date").getTime() : 0;
		loc = p.needCoords ? new Location(p.world, rs.getInt("x"), rs.getInt("y"), rs.getInt("z")) : null;
		playerName = p.needPlayer ? rs.getString("playername") : null;
		beforeID = p.needType ? rs.getInt("replaced") : 0;
		afterID = p.needType ? rs.getInt("type") : 0;
		data = p.needData ? rs.getByte("data") : (byte)0;
		signtext = p.needSignText ? rs.getString("signtext") : null;
		itemStack = p.needChestAccess ? ConversionUtil.grabFromRS(rs) : null;
	}

	@Override
	public String toString() {
		final StringBuilder msg = new StringBuilder();
		if (date > 0)
			msg.append(Config.formatter.format(date)).append(" ");
		if (playerName != null)
			msg.append(playerName).append(" ");
		if (signtext != null) {
			final String action = afterID == 0 ? "destroyed " : "created ";
			if (!signtext.contains("\0"))
				msg.append(action).append(signtext);
			else
				msg.append(action).append(materialName(afterID != 0 ? afterID : beforeID)).append(" [").append(signtext.replace("\0", "] [")).append("]");
		} else if (afterID == beforeID) {
			if (afterID == 0)
				msg.append("did an unspecified action");
			else if (itemStack != null) {
				ItemStack is = itemStack.toBukkit();
				if (!itemStack.wasAdded())
					msg.append("took ").append(is.getAmount()).append("x ").append(materialName(is.getTypeId(), is.getDurability())).append(" from ").append(materialName(afterID));
				else
					msg.append("put ").append(is.getAmount()).append("x ").append(materialName(is.getTypeId(), is.getDurability())).append(" into ").append(materialName(afterID));
			} else if (BukkitUtils.getContainerBlocks().contains(Material.getMaterial(afterID)))
				msg.append("opened ").append(materialName(afterID));
			else if (afterID == 64 || afterID == 71)
				// This is a problem that will have to be addressed in LB 2,
				// there is no way to tell from the top half of the block if
				// the door is opened or closed.
				msg.append("moved ").append(materialName(afterID));
			// Trapdoor
			else if (afterID == 96)
				msg.append((data < 8 || data > 11) ? "opened" : "closed").append(" ").append(materialName(afterID));
			// Fence gate
			else if (afterID == 107)
				msg.append(data > 3 ? "opened" : "closed").append(" ").append(materialName(afterID));
			else if (afterID == 69)
				msg.append("switched ").append(materialName(afterID));
			else if (afterID == 77 || afterID == 143)
				msg.append("pressed ").append(materialName(afterID));
			else if (afterID == 92)
				msg.append("ate a piece of ").append(materialName(afterID));
			else if (afterID == 25 || afterID == 93 || afterID == 94 || afterID == 149 || afterID == 150)
				msg.append("changed ").append(materialName(afterID));
			else if (afterID == 70 || afterID == 72 || afterID == 147 || afterID == 148)
				msg.append("stepped on ").append(materialName(afterID));
			else if (afterID == 132)
				msg.append("ran into ").append(materialName(afterID));
		} else if (afterID == 0)
			msg.append("destroyed ").append(materialName(beforeID, data));
		else if (beforeID == 0)
			msg.append("created ").append(materialName(afterID, data));
		else
			msg.append("replaced ").append(materialName(beforeID, (byte)0)).append(" with ").append(materialName(afterID, data));
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
