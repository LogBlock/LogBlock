package de.diddiz.LogBlock;

import de.diddiz.LogBlock.util.MessagingUtil;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
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
    public BaseComponent getLogMessage(int entry) {
        return MessagingUtil.formatSummarizedChanges(kills, killed, new TextComponent(player.getName()), 6, 7, spaceFactor);
    }

    @Override
    public int getNumChanges() {
        return kills + killed;
    }
}
