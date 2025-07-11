package de.diddiz.LogBlock;

import de.diddiz.LogBlock.componentwrapper.Component;
import de.diddiz.LogBlock.componentwrapper.Components;
import de.diddiz.LogBlock.util.MessagingUtil;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.Location;

public class SummedKills implements LookupCacheElement {
    private final Actor player;
    private final int kills, killed;
    private final float spaceFactor;

    public SummedKills(ResultSet rs, QueryParams p, float spaceFactor) throws SQLException {
        player = new Actor(rs);
        kills = rs.getInt("kills");
        killed = rs.getInt("killed");
        this.spaceFactor = spaceFactor;
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public Component getLogMessage(int entry) {
        return MessagingUtil.formatSummarizedChanges(kills, killed, Components.text(player.getName()), 6, 7, spaceFactor);
    }

    @Override
    public int getNumChanges() {
        return kills + killed;
    }
}
