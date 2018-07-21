package de.diddiz.LogBlock;

import org.bukkit.Material;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;

public class Tool {
    public final String name;
    public final List<String> aliases;
    public final ToolBehavior leftClickBehavior, rightClickBehavior;
    public final boolean defaultEnabled;
    public final Material item;
    public final boolean canDrop;
    public final QueryParams params;
    public final ToolMode mode;
    public final PermissionDefault permissionDefault;

    public Tool(String name, List<String> aliases, ToolBehavior leftClickBehavior, ToolBehavior rightClickBehavior, boolean defaultEnabled, Material item, boolean canDrop, QueryParams params, ToolMode mode, PermissionDefault permissionDefault) {
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
