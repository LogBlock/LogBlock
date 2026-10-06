package de.diddiz.LogBlock;

public enum ToolMode {
    CLEARLOG("logblock.clearlog"),
    LOOKUP("logblock.lookup"),
    REDO("logblock.rollback"),
    ROLLBACK("logblock.rollback"),
    WRITELOGFILE("logblock.rollback");
    private final String permission;

    private ToolMode(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }
}
