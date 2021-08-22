package de.diddiz.LogBlock;

public enum Logging {
    BLOCKPLACE(true),
    BLOCKBREAK(true),
    SIGNTEXT(true),
    TNTEXPLOSION(true),
    CREEPEREXPLOSION(true),
    GHASTFIREBALLEXPLOSION(true),
    ENDERDRAGON(true),
    MISCEXPLOSION(true),
    FIRE(true),
    LEAVESDECAY,
    LAVAFLOW,
    WATERFLOW,
    CHESTACCESS,
    KILL,
    CHAT,
    SNOWFORM,
    SNOWFADE,
    DOORINTERACT,
    SWITCHINTERACT,
    CAKEEAT,
    ENDERMEN,
    NOTEBLOCKINTERACT,
    DIODEINTERACT,
    COMPARATORINTERACT,
    PRESUREPLATEINTERACT,
    TRIPWIREINTERACT,
    CAULDRONINTERACT(true),
    CREATURECROPTRAMPLE,
    CROPTRAMPLE,
    NATURALSTRUCTUREGROW,
    GRASSGROWTH,
    MYCELIUMSPREAD,
    VINEGROWTH,
    DRIPSTONEGROWTH,
    MUSHROOMSPREAD,
    BAMBOOGROWTH,
    WITHER(true),
    WITHER_SKULL(true),
    BONEMEALSTRUCTUREGROW,
    WORLDEDIT,
    TNTMINECARTEXPLOSION(true),
    ENDERCRYSTALEXPLOSION(true),
    BEDEXPLOSION(true),
    DRAGONEGGTELEPORT(true),
    DAYLIGHTDETECTORINTERACT,
    LECTERNBOOKCHANGE(true),
    SCAFFOLDING(true),
    OXIDIZATION,
    SHULKER_BOX_CONTENT,
    PLAYER_COMMANDS,
    COMMANDBLOCK_COMMANDS,
    CONSOLE_COMMANDS,
    REPEATER;
    public static final int length = Logging.values().length;
    private final boolean defaultEnabled;

    private Logging() {
        this(false);
    }

    private Logging(boolean defaultEnabled) {
        this.defaultEnabled = defaultEnabled;
    }

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }
}
