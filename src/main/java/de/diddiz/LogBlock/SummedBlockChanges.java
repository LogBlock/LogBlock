package de.diddiz.LogBlock;

import de.diddiz.LogBlock.QueryParams.SummarizationMode;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;

import static de.diddiz.util.ActionColor.CREATE;
import static de.diddiz.util.ActionColor.DESTROY;
import static de.diddiz.util.MaterialName.materialName;
import static de.diddiz.util.MessagingUtil.prettyMaterial;
import static de.diddiz.util.Utils.spaces;

public class SummedBlockChanges implements LookupCacheElement
{
	private final String group;
	private final int created, destroyed;
	private final float spaceFactor;

	public SummedBlockChanges(ResultSet rs, QueryParams p, float spaceFactor) throws SQLException {
		group = p.sum == SummarizationMode.PLAYERS ? rs.getString(1) : materialName(rs.getInt(1));
		created = rs.getInt(2);
		destroyed = rs.getInt(3);
		this.spaceFactor = spaceFactor;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getMessage() {
		StringBuilder builder = new StringBuilder();
		builder.append(CREATE).append(created).append(spaces((int) ((10 - String.valueOf(created).length()) / spaceFactor)));
		builder.append(DESTROY).append(destroyed).append(spaces((int)((10 - String.valueOf(destroyed).length()) / spaceFactor)));
		builder.append(prettyMaterial(group.toUpperCase()));
		return builder.toString();
	}
}
