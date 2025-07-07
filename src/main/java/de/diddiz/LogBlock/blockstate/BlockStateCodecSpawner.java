package de.diddiz.LogBlock.blockstate;

import de.diddiz.LogBlock.LogBlock;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

public class BlockStateCodecSpawner implements BlockStateCodec {
    @Override
    public Material[] getApplicableMaterials() {
        return new Material[] { Material.SPAWNER };
    }

    @Override
    public YamlConfiguration serialize(BlockState state) {
        if (state instanceof CreatureSpawner) {
            CreatureSpawner spawner = (CreatureSpawner) state;
            YamlConfiguration conf = new YamlConfiguration();
            conf.set("delay", spawner.getDelay());
            conf.set("maxNearbyEntities", spawner.getMaxNearbyEntities());
            conf.set("maxSpawnDelay", spawner.getMaxSpawnDelay());
            conf.set("minSpawnDelay", spawner.getMinSpawnDelay());
            conf.set("requiredPlayerRange", spawner.getRequiredPlayerRange());
            conf.set("spawnCount", spawner.getSpawnCount());
            if (spawner.getSpawnedType() != null) {
                conf.set("spawnedType", spawner.getSpawnedType().name());
            }
            conf.set("spawnRange", spawner.getSpawnRange());
            return conf;
        }
        return null;
    }

    @Override
    public void deserialize(BlockState state, YamlConfiguration conf) {
        if (state instanceof CreatureSpawner) {
            CreatureSpawner spawner = (CreatureSpawner) state;
            if (conf != null) {
                spawner.setDelay(conf.getInt("delay"));
                spawner.setMaxNearbyEntities(conf.getInt("maxNearbyEntities"));
                spawner.setMaxSpawnDelay(conf.getInt("maxSpawnDelay"));
                spawner.setMinSpawnDelay(conf.getInt("minSpawnDelay"));
                spawner.setRequiredPlayerRange(conf.getInt("requiredPlayerRange"));
                spawner.setSpawnCount(conf.getInt("spawnCount"));
                EntityType spawnedType = null;
                String spawnedTypeString = conf.getString("spawnedType");
                if (spawnedTypeString != null) {
                    try {
                        spawnedType = EntityType.valueOf(spawnedTypeString);
                    } catch (IllegalArgumentException ignored) {
                        LogBlock.getInstance().getLogger().warning("Could not find spawner spawned type: " + spawnedTypeString);
                    }
                }
                spawner.setSpawnedType(spawnedType);
                spawner.setSpawnRange(conf.getInt("spawnRange"));
            }
        }
    }

    @Override
    public BaseComponent getChangesAsComponent(YamlConfiguration conf, YamlConfiguration oldState) {
        if (conf != null) {
            String spawnedTypeString = conf.getString("spawnedType");
            if (spawnedTypeString != null) {
                EntityType entity = EntityType.valueOf(spawnedTypeString);
                if (entity != null) {
                    return new TextComponent("[" + entity.getKey().getKey() + "]");
                }
            }
        }
        return null;
    }
}
