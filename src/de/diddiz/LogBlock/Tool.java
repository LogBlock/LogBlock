package de.diddiz.LogBlock;

import java.util.List;
import org.bukkit.permissions.PermissionDefault;

public class Tool
{
	public final String name;
	public final List<String> aliases;
	public final ToolBehavior leftClickBehavior, rightClickBehavior;
	public final boolean defaultEnabled;
	public final int item;
	public final boolean canDrop;
	public final QueryParams params;
	public final ToolMode mode;
	public final PermissionDefault permissionDefault;

	public Tool(String name, List<String> aliases, ToolBehavior leftClickBehavior, ToolBehavior rightClickBehavior, boolean defaultEnabled, int item, boolean canDrop, QueryParams params, ToolMode mode, PermissionDefault permissionDefault) {
		this.name = name;
		this.aliases = aliases;
		this.leftClickBehavior = leftClickBehavior;
		this.rightClickBehavior = rightClickBehavior;
		this.defaultEnabled = defaultEnabled;
		this.item = item;
		this.canDrop = canDrop;
		this.params = params;
		this.mode = mode;
		this.permissionDefault = permissionDefault;
	}
}
