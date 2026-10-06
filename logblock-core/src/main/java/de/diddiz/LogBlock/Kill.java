package de.diddiz.LogBlock;

import static de.diddiz.LogBlock.util.ActionColor.DESTROY;
import static de.diddiz.LogBlock.util.MessagingUtil.prettyDate;
import static de.diddiz.LogBlock.util.MessagingUtil.prettyLocation;
import static de.diddiz.LogBlock.util.MessagingUtil.prettyMaterial;

import de.diddiz.LogBlock.componentwrapper.Component;
import de.diddiz.LogBlock.componentwrapper.Components;
import de.diddiz.LogBlock.util.BukkitUtils;
import de.diddiz.LogBlock.util.MessagingUtil;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.Location;
import org.bukkit.Material;

public class Kill implements LookupCacheElement {
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
        id = p.needId ? rs.getLong("id") : 0;
        date = p.needDate ? rs.getTimestamp("date").getTime() : 0;
        loc = p.needCoords ? new Location(p.world, rs.getInt("x"), rs.getInt("y"), rs.getInt("z")) : null;
        killerName = p.needKiller ? rs.getString("killer") : null;
        victimName = p.needVictim ? rs.getString("victim") : null;
        weapon = p.needWeapon ? rs.getInt("weapon") : 0;
    }

    @Override
    public String toString() {
        return Components.toPlainText(getLogMessage());
    }

    @Override
    public Location getLocation() {
        return loc;
    }

    @Override
    public Component getLogMessage(int entry) {
        Component msg = Components.empty();
        if (date > 0) {
            msg = msg.append(prettyDate(date));
            msg = msg.append(" ");
        }
        msg = msg.append(MessagingUtil.createTextComponentWithColor(killerName + " killed ", DESTROY.getColor()));
        msg = msg.append(Components.text(victimName));
        if (loc != null) {
            msg = msg.append(" at ");
            msg = msg.append(prettyLocation(loc, entry));
        }
        if (weapon != 0) {
            msg = msg.append(" with ");
            msg = msg.append(prettyItemName(MaterialConverter.getMaterial(weapon)));
        }
        return msg;
    }

    public Component prettyItemName(Material t) {
        if (t == null || BukkitUtils.isEmpty(t)) {
            return prettyMaterial("fist");
        }
        return prettyMaterial(t.toString().replace('_', ' '));
    }
}
