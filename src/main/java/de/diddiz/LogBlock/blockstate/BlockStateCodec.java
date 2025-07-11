package de.diddiz.LogBlock.blockstate;

import de.diddiz.LogBlock.componentwrapper.Component;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.YamlConfiguration;

public interface BlockStateCodec {
    Material[] getApplicableMaterials();

    YamlConfiguration serialize(BlockState state);

    void deserialize(BlockState state, YamlConfiguration conf);

    Component getChangesAsComponent(YamlConfiguration conf, YamlConfiguration oldState);
}
