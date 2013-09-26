package de.diddiz.LogBlock;

public enum Logging
{
	BLOCKPLACE(true), BLOCKBREAK(true), SIGNTEXT, TNTEXPLOSION(true), CREEPEREXPLOSION(true),
	GHASTFIREBALLEXPLOSION(true), ENDERDRAGON(true), MISCEXPLOSION, FIRE(true), LEAVESDECAY,
	LAVAFLOW, WATERFLOW, CHESTACCESS, KILL, CHAT, SNOWFORM, SNOWFADE, DOORINTERACT,
	SWITCHINTERACT, CAKEEAT, ENDERMEN, NOTEBLOCKINTERACT, DIODEINTERACT, COMPARATORINTERACT,
	PRESUREPLATEINTERACT, TRIPWIREINTERACT, CREATURECROPTRAMPLE, CROPTRAMPLE,
	NATURALSTRUCTUREGROW, GRASSGROWTH, MYCELIUMSPREAD, VINEGROWTH, MUSHROOMSPREAD,
	WITHER(true), WITHER_SKULL(true), BONEMEALSTRUCTUREGROW,
	WORLDEDIT, TNTMINECARTEXPLOSION(true), LOCKEDCHESTDECAY;

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
