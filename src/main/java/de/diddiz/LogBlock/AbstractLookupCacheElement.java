package de.diddiz.LogBlock;

import de.diddiz.LogBlock.html.Parser;

public abstract class AbstractLookupCacheElement implements LookupCacheElement
{
	public String getXmlMessage() {
		return Parser.escape(getMessage());
	}
}
