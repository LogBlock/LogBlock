package de.diddiz.LogBlock.events;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.ChestAccess;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;

public class BlockChangePreLogEvent extends PreLogEvent {

    private static final HandlerList handlers = new HandlerList();
    private Location location;
    private BlockData typeBefore, typeAfter;
    private ChestAccess chestAccess;
    private YamlConfiguration stateBefore;
    private YamlConfiguration stateAfter;

    public BlockChangePreLogEvent(Actor owner, Location location, BlockData typeBefore, BlockData typeAfter, YamlConfiguration stateBefore, YamlConfiguration stateAfter, ChestAccess chestAccess) {
        super(owner);
        this.location = location;
        this.typeBefore = typeBefore;
        this.typeAfter = typeAfter;
        this.stateBefore = stateBefore;
        this.stateAfter = stateAfter;
        this.chestAccess = chestAccess;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public BlockData getTypeBefore() {
        return typeBefore;
    }

    public void setTypeBefore(BlockData typeBefore) {
        if (typeBefore == null) {
            typeBefore = Bukkit.createBlockData(Material.AIR);
        }
        this.typeBefore = typeBefore;
    }

    public BlockData getTypeAfter() {
        return typeAfter;
    }

    public void setTypeAfter(BlockData typeAfter) {
        if (typeAfter == null) {
            typeAfter = Bukkit.createBlockData(Material.AIR);
        }
        this.typeAfter = typeAfter;
    }

    public YamlConfiguration getStateBefore() {
        return stateBefore;
    }

    public YamlConfiguration getStateAfter() {
        return stateAfter;
    }

    public void setStateBefore(YamlConfiguration stateBefore) {
        this.stateBefore = stateBefore;
    }

    public void setStateAfter(YamlConfiguration stateAfter) {
        this.stateAfter = stateAfter;
    }

    public ChestAccess getChestAccess() {
        return chestAccess;
    }

    public void setChestAccess(ChestAccess chestAccess) {
        this.chestAccess = chestAccess;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
