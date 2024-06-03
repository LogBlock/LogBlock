package de.diddiz.LogBlock;

import static de.diddiz.LogBlock.util.MessagingUtil.prettyMaterial;

import de.diddiz.LogBlock.QueryParams.SummarizationMode;
import de.diddiz.LogBlock.util.MessagingUtil;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public class SummedEntityChanges implements LookupCacheElement {
    private final int type;
    private final int created, destroyed;
    private final float spaceFactor;
    private final Actor actor;

    public SummedEntityChanges(ResultSet rs, QueryParams p, float spaceFactor) throws SQLException {
        // Actor currently useless here as we don't yet output UUID in results anywhere
        actor = p.sum == SummarizationMode.PLAYERS ? new Actor(rs) : null;
        type = p.sum == SummarizationMode.TYPES ? rs.getInt("entitytypeid") : 0;
        created = rs.getInt("created");
        destroyed = rs.getInt("destroyed");
        this.spaceFactor = spaceFactor;
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public BaseComponent getLogMessage(int entry) {
        return MessagingUtil.formatSummarizedChanges(created, destroyed, actor != null ? new TextComponent(actor.getName()) : prettyMaterial(Objects.toString(EntityTypeConverter.getEntityType(type))), 10, 10, spaceFactor);
    }

    @Override
    public int getNumChanges() {
        return created + destroyed;
    }
}
