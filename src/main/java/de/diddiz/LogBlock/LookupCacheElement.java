package de.diddiz.LogBlock;

import org.bukkit.Location;

public interface LookupCacheElement
{
	public Location getLocation();

	public String getMessage();

	String getXmlMessage();
}
