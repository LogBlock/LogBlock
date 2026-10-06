package de.diddiz.LogBlock.events;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.EntityChange.EntityChangeType;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;

public class EntityChangePreLogEvent extends PreLogEvent {

    private static final HandlerList handlers = new HandlerList();
    private Location location;
    private Entity entity;
    private EntityChangeType changeType;
    private YamlConfiguration changeData;

    public EntityChangePreLogEvent(Actor owner, Location location, Entity entity, EntityChangeType changeType, YamlConfiguration changeData) {
        super(owner);
        this.location = location;
        this.entity = entity;
        this.changeType = changeType;
        this.changeData = changeData;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Entity getEntity() {
        return entity;
    }

    public EntityChangeType getChangeType() {
        return changeType;
    }

    public YamlConfiguration getChangeData() {
        return changeData;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
