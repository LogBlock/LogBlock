package de.diddiz.LogBlock;

import static de.diddiz.util.MaterialName.materialName;
import static de.diddiz.util.Utils.spaces;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Location;
import de.diddiz.LogBlock.QueryParams.SummarizationMode;

public class SummedBlockChanges extends AbstractLookupCacheElement
{
	private final String group;
	private final int created, destroyed;
	private final float spaceFactor;
	private final boolean isPlayers;

	public SummedBlockChanges(ResultSet rs, QueryParams p, float spaceFactor) throws SQLException {
		isPlayers = p.sum == SummarizationMode.PLAYERS;
		group = isPlayers ? rs.getString(1) : materialName(rs.getInt(1));
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
		return created + spaces((int)((10 - String.valueOf(created).length()) / spaceFactor)) + destroyed + spaces((int)((10 - String.valueOf(destroyed).length()) / spaceFactor)) + group;
	}

	@Override
	public String getXmlMessage() {
		String message = created + spaces((int) ((10 - String.valueOf(created).length()) / spaceFactor)) + destroyed + spaces((int) ((10 - String.valueOf(destroyed).length()) / spaceFactor));

		final String command = String.format("/lb last %s %s sum none coords", isPlayers ? "player" : "block", group.replace(' ', '_'));
		message = String.format("%1$s<span onClick=\"run_command('%2$s')\" onHover=\"show_text('Click to run\n&lt;color name=&quot;blue&quot;>%2$s&lt;/color>')\">%3$s</span>", message, command, group);
		if (created < destroyed) {
			message = String.format("<color name=\"red\">%1$s</color>", message);
		}

		return message;
	}
}
