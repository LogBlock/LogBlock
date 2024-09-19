package de.diddiz.LogBlock.blockstate;

import java.util.List;
import java.util.Locale;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Banner;
import org.bukkit.block.BlockState;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class BlockStateCodecBanner implements BlockStateCodec {
    @Override
    public Material[] getApplicableMaterials() {
        return new Material[] { Material.WHITE_BANNER, Material.ORANGE_BANNER, Material.MAGENTA_BANNER, Material.LIGHT_BLUE_BANNER, Material.YELLOW_BANNER, Material.LIME_BANNER, Material.PINK_BANNER, Material.GRAY_BANNER, Material.LIGHT_GRAY_BANNER, Material.CYAN_BANNER, Material.PURPLE_BANNER,
                Material.BLUE_BANNER, Material.BROWN_BANNER, Material.GREEN_BANNER, Material.RED_BANNER, Material.BLACK_BANNER, Material.WHITE_WALL_BANNER, Material.ORANGE_WALL_BANNER, Material.MAGENTA_WALL_BANNER, Material.LIGHT_BLUE_WALL_BANNER, Material.YELLOW_WALL_BANNER,
                Material.LIME_WALL_BANNER, Material.PINK_WALL_BANNER, Material.GRAY_WALL_BANNER, Material.LIGHT_GRAY_WALL_BANNER, Material.CYAN_WALL_BANNER, Material.PURPLE_WALL_BANNER, Material.BLUE_WALL_BANNER, Material.BROWN_WALL_BANNER, Material.GREEN_WALL_BANNER, Material.RED_WALL_BANNER,
                Material.BLACK_WALL_BANNER };
    }

    @Override
    public YamlConfiguration serialize(BlockState state) {
        if (state instanceof Banner) {
            Banner banner = (Banner) state;
            int nr = 0;
            List<Pattern> patterns = banner.getPatterns();
            if (!patterns.isEmpty()) {
                YamlConfiguration conf = new YamlConfiguration();
                ConfigurationSection patternsSection = conf.createSection("patterns");
                for (Pattern pattern : patterns) {
                    NamespacedKey key = pattern.getPattern().getKey();
                    if (key != null) {
                        ConfigurationSection section = patternsSection.createSection(Integer.toString(nr));
                        section.set("color", pattern.getColor().name());
                        section.set("pattern", key.toString());
                        nr++;
                    }
                }
                return conf;
            }
        }
        return null;
    }

    @Override
    public void deserialize(BlockState state, YamlConfiguration conf) {
        if (state instanceof Banner) {
            Banner banner = (Banner) state;
            int oldPatterns = banner.getPatterns().size();
            for (int i = 0; i < oldPatterns; i++) {
                banner.removePattern(0);
            }
            ConfigurationSection patternsSection = conf == null ? null : conf.getConfigurationSection("patterns");
            if (patternsSection != null) {
                for (String key : patternsSection.getKeys(false)) {
                    ConfigurationSection section = patternsSection.getConfigurationSection(key);
                    if (section != null) {
                        DyeColor color = DyeColor.valueOf(section.getString("color"));
                        NamespacedKey patternKey = NamespacedKey.fromString(section.getString("pattern").toLowerCase(Locale.ROOT));
                        if (patternKey != null) {
                            PatternType type = Registry.BANNER_PATTERN.get(patternKey);
                            if (type != null) {
                                banner.addPattern(new Pattern(color, type));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public BaseComponent getChangesAsComponent(YamlConfiguration conf, YamlConfiguration oldState) {
        return null;
    }
}
