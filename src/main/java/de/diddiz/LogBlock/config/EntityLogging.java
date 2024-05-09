package de.diddiz.LogBlock.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.EntityType;

public enum EntityLogging {
    SPAWN(new String[] { EntityType.ARMOR_STAND.name(), EntityType.ITEM_FRAME.name(), EntityType.SNOW_GOLEM.name() }),
    DESTROY(new String[] { EntityType.ARMOR_STAND.name(), EntityType.ITEM_FRAME.name(), EntityType.VILLAGER.name(), EntityType.SNOW_GOLEM.name(), "ANIMAL" }),
    MODIFY(new String[] { "ALL" });

    public static final int length = EntityLogging.values().length;
    private final List<String> defaultEnabled;

    private EntityLogging() {
        this(null);
    }

    private EntityLogging(String[] defaultEnabled) {
        this.defaultEnabled = defaultEnabled == null ? Collections.emptyList() : Collections.unmodifiableList(Arrays.asList(defaultEnabled));
    }

    public List<String> getDefaultEnabled() {
        return defaultEnabled;
    }
}
