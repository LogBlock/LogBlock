package de.diddiz.LogBlock.events;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public abstract class PreLogEvent extends Event implements Cancellable {

	protected boolean cancelled = false;
	protected String owner;

	public PreLogEvent(String owner) {

		this.owner = owner.replaceAll("[^a-zA-Z0-9_]", "");
	}

	/**
	 * Returns the player/monster/cause involved in this event
	 *
	 * @return Player/monster/cause who is involved in this event
	 */
	public String getOwner() {

		return owner;
	}

	/**
	 * Sets the player/monster/cause involved in this event
	 *
	 * @param owner The player/monster/cause who is involved in this event
	 */
	public void setOwner(String owner) {

		this.owner = owner.replaceAll("[^a-zA-Z0-9_]", "");
	}

	public boolean isCancelled() {

		return cancelled;
	}

	public void setCancelled(boolean cancelled) {

		this.cancelled = cancelled;
	}
}
