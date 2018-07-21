package de.diddiz.LogBlock;

import de.diddiz.LogBlock.QueryParams.SummarizationMode;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;

import static de.diddiz.util.Utils.spaces;

public class SummedBlockChanges implements LookupCacheElement {
    private final int type;
    private final int created, destroyed;
    private final float spaceFactor;
    private final Actor actor;

    public SummedBlockChanges(ResultSet rs, QueryParams p, float spaceFactor) throws SQLException {
        // Actor currently useless here as we don't yet output UUID in results anywhere
        actor = p.sum == SummarizationMode.PLAYERS ? new Actor(rs) : null;
        type = p.sum == SummarizationMode.TYPES ? rs.getInt("type") : 0;
        created = rs.getInt("created");
        destroyed = rs.getInt("destroyed");
        this.spaceFactor = spaceFactor;
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public String getMessage() {
        return created + spaces((int) ((10 - String.valueOf(created).length()) / spaceFactor)) + destroyed + spaces((int) ((10 - String.valueOf(destroyed).length()) / spaceFactor)) + (actor != null ? actor.getName() : MaterialConverter.getMaterial(type).toString());
    }
}
