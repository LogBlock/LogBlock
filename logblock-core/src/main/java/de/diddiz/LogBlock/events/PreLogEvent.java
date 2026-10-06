package de.diddiz.LogBlock.events;

import de.diddiz.LogBlock.Actor;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public abstract class PreLogEvent extends Event implements Cancellable {

    protected boolean cancelled = false;
    protected Actor owner;

    public PreLogEvent(Actor owner) {
        this.owner = owner;
    }

    /**
     * Returns the player/monster/cause involved in this event
     *
     * @return Player/monster/cause who is involved in this event
     * @deprecated {@link #getOwnerActor() } returns an object encapsulating
     * name and uuid.  Names are not guaranteed to be unique.
     */
    @Deprecated
    public String getOwner() {
        return owner.getName();
    }

    /**
     * Returns the player/monster/cause involved in this event
     *
     * @return Player/monster/cause who is involved in this event
     */
    public Actor getOwnerActor() {
        return owner;
    }

    /**
     * Sets the player/monster/cause involved in this event
     *
     * @param owner The player/monster/cause who is involved in this event
     */
    public void setOwner(Actor owner) {
        this.owner = owner;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
