package de.diddiz.LogBlock.events;

import org.bukkit.event.HandlerList;

public class ChatPreLogEvent extends PreLogEvent {

	private static final HandlerList handlers = new HandlerList();
	private String message;

	public ChatPreLogEvent(String owner, String message) {

		super(owner);
		this.message = message;
	}

	public String getMessage() {

		return message;
	}

	public void setMessage(String message) {

		this.message = message;
	}

	public HandlerList getHandlers() {

		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}
}