package de.diddiz.LogBlock;

import org.bukkit.entity.Player;

public class ToolData {
    public boolean enabled;
    public QueryParams params;
    public ToolMode mode;

    public ToolData(Tool tool, LogBlock logblock, Player player) {
        enabled = tool.defaultEnabled && logblock.hasPermission(player, "logblock.tools." + tool.name);
        params = tool.params.clone();
        mode = tool.mode;
    }
}
