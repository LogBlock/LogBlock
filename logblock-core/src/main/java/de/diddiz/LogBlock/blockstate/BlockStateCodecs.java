package de.diddiz.LogBlock.blockstate;

import de.diddiz.LogBlock.componentwrapper.Component;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.persistence.PersistentDataContainer;

public class BlockStateCodecs {
    private static Map<Material, BlockStateCodec> codecs = new HashMap<>();

    public static void registerCodec(BlockStateCodec codec) {
        Material[] materials = codec.getApplicableMaterials();
        for (Material material : materials) {
            if (codecs.containsKey(material)) {
                throw new IllegalArgumentException("BlockStateCodec for " + material + " already registered!");
            }
            codecs.put(material, codec);
        }
    }

    static {
        registerCodec(new BlockStateCodecSign());
        registerCodec(new BlockStateCodecSkull());
        registerCodec(new BlockStateCodecBanner());
        registerCodec(new BlockStateCodecSpawner());
        registerCodec(new BlockStateCodecLectern());
        registerCodec(new BlockStateCodecShulkerBox());
    }

    public static boolean hasCodec(Material material) {
        return codecs.containsKey(material);
    }

    public static YamlConfiguration serialize(BlockState state) {
        BlockStateCodec codec = codecs.get(state.getType());
        YamlConfiguration serialized = codec == null ? null : codec.serialize(state);
        if (state instanceof TileState tile) {
            PersistentDataContainer pdc = tile.getPersistentDataContainer();
            if (!pdc.isEmpty()) {
                if (serialized == null) {
                    serialized = new YamlConfiguration();
                }
                serialized.set(PersistentDataContainerCodec.KEY, PersistentDataContainerCodec.serialize(pdc));
            }
        }
        return serialized == null || serialized.getKeys(false).isEmpty() ? null : serialized;
    }

    public static void deserialize(BlockState block, YamlConfiguration state) {
        BlockStateCodec codec = codecs.get(block.getType());
        if (codec != null) {
            codec.deserialize(block, state);
        }
        if (block instanceof TileState tile && state != null && state.contains(PersistentDataContainerCodec.KEY)) {
            PersistentDataContainerCodec.deserialize(tile.getPersistentDataContainer(), state.get(PersistentDataContainerCodec.KEY));
        }
    }

    public static Component getChangesAsComponent(Material material, YamlConfiguration state, YamlConfiguration oldState) {
        BlockStateCodec codec = codecs.get(material);
        if (codec != null) {
            return codec.getChangesAsComponent(state, oldState);
        }
        return null;
    }
}
