package de.diddiz.worldedit;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class RegionContainer {

    private Region selection;

    public RegionContainer(Region sel) {
        this.selection = sel;
    }

    public static RegionContainer fromPlayerSelection(Player player, Plugin plugin) {
        LocalSession session = ((WorldEditPlugin) plugin).getSession(player);
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(player.getWorld());
        if (!weWorld.equals(session.getSelectionWorld())) {
            throw new IllegalArgumentException("No selection defined");
        }
        Region selection;
        try {
            selection = session.getSelection(weWorld);
        } catch (IncompleteRegionException e) {
            throw new IllegalArgumentException("No selection defined");
        }
        if (selection == null) {
            throw new IllegalArgumentException("No selection defined");
        }
        if (!(selection instanceof CuboidRegion)) {
            throw new IllegalArgumentException("You have to define a cuboid selection");
        }
        return new RegionContainer(selection);
    }

    public static RegionContainer fromCorners(World world, Location first, Location second) {
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
        Vector firstVector = BukkitAdapter.asVector(first);
        Vector secondVector = BukkitAdapter.asVector(second);
        
        return new RegionContainer(new CuboidRegion(weWorld, firstVector, secondVector));
    }

    public Region getSelection() {
        return selection;
    }

    public void setSelection(Region selection) {
        this.selection = selection;
    }
}
