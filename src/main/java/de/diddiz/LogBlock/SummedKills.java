package de.diddiz.LogBlock;

import static de.diddiz.util.MaterialName.materialName;
import static de.diddiz.util.Utils.spaces;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.Location;
import de.diddiz.LogBlock.QueryParams.SummarizationMode;

public class SummedKills implements LookupCacheElement
{
	private final String playerName;
	private final int kills, killed;
	private final float spaceFactor;

	public SummedKills(ResultSet rs, QueryParams p, float spaceFactor) throws SQLException {
		playerName = rs.getString(1);
		kills = rs.getInt(2);
		killed = rs.getInt(3);
		this.spaceFactor = spaceFactor;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getMessage() {
		return kills + spaces((int)((6 - String.valueOf(kills).length()) / spaceFactor)) + killed + spaces((int)((7 - String.valueOf(killed).length()) / spaceFactor)) + playerName;
	}
}
