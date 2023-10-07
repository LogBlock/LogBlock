package de.diddiz.LogBlock.blockstate;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;

import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;
import de.diddiz.LogBlock.util.BukkitUtils;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class BlockStateCodecShulkerBox implements BlockStateCodec {
    @Override
    public Material[] getApplicableMaterials() {
        return BukkitUtils.getShulkerBoxBlocks().toArray(Material[]::new);
    }

    @Override
    public YamlConfiguration serialize(BlockState state) {
        WorldConfig wcfg = getWorldConfig(state.getWorld());
        if (wcfg == null || !wcfg.isLogging(Logging.SHULKER_BOX_CONTENT)) {
            return null;
        }
        if (state instanceof ShulkerBox) {
            ShulkerBox shulkerBox = (ShulkerBox) state;
            ItemStack[] content = shulkerBox.getSnapshotInventory().getStorageContents();
            YamlConfiguration conf = new YamlConfiguration();
            boolean anySlot = false;
            for (int i = 0; i < content.length; i++) {
                ItemStack stack = content[i];
                if (stack != null && stack.getType() != Material.AIR) {
                    conf.set("slot" + i, stack);
                    anySlot = true;
                }
            }
            if (anySlot) {
                return conf;
            }
        }
        return null;
    }

    @Override
    public void deserialize(BlockState state, YamlConfiguration conf) {
        if (state instanceof ShulkerBox) {
            ShulkerBox shulkerBox = (ShulkerBox) state;
            if (conf != null) {
                ItemStack[] content = shulkerBox.getSnapshotInventory().getStorageContents();
                for (int i = 0; i < content.length; i++) {
                    ItemStack stack = conf.getItemStack("slot" + i);
                    if (stack != null && stack.getType() != Material.AIR) {
                        content[i] = stack;
                    }
                }
                shulkerBox.getSnapshotInventory().setContents(content);
            }
        }
    }

    @Override
    public BaseComponent getChangesAsComponent(YamlConfiguration conf, YamlConfiguration oldState) {
        if (conf != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean anySlot = false;
            for (String key : conf.getKeys(false)) {
                if (key.startsWith("slot")) {
                    ItemStack stack = conf.getItemStack(key);
                    if (stack != null && stack.getType() != Material.AIR) {
                        if (anySlot) {
                            sb.append(",");
                        }
                        anySlot = true;
                        sb.append(stack.getAmount()).append("x").append(stack.getType());
                    }
                }
            }
            sb.append("]");
            return anySlot ? new TextComponent(sb.toString()) : null;
        }
        return null;
    }
}
