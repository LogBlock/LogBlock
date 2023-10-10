package de.diddiz.LogBlock.blockstate;

import de.diddiz.LogBlock.util.BukkitUtils;
import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class BlockStateCodecSign implements BlockStateCodec {
    @Override
    public Material[] getApplicableMaterials() {
        return BukkitUtils.getAllSignMaterials().toArray(new Material[BukkitUtils.getAllSignMaterials().size()]);
    }

    @Override
    public YamlConfiguration serialize(BlockState state) {
        YamlConfiguration conf = null;
        if (state instanceof Sign sign) {
            boolean waxed = sign.isWaxed();
            if (waxed) {
                conf = new YamlConfiguration();
                conf.set("waxed", waxed);
            }
            for (Side side : Side.values()) {
                SignSide signSide = sign.getSide(side);
                String[] lines = signSide.getLines();
                boolean hasText = false;
                for (int i = 0; i < lines.length; i++) {
                    if (lines[i] != null && lines[i].length() > 0) {
                        hasText = true;
                        break;
                    }
                }
                DyeColor signColor = signSide.getColor();
                if (signColor == null) {
                    signColor = DyeColor.BLACK;
                }
                boolean glowing = signSide.isGlowingText();
                if (hasText || signColor != DyeColor.BLACK || glowing) {
                    if (conf == null) {
                        conf = new YamlConfiguration();
                    }
                    ConfigurationSection sideSection = side == Side.FRONT ? conf : conf.createSection(side.name().toLowerCase());
                    if (hasText) {
                        sideSection.set("lines", Arrays.asList(lines));
                    }
                    if (signColor != DyeColor.BLACK) {
                        sideSection.set("color", signColor.name());
                    }
                    if (glowing) {
                        sideSection.set("glowing", true);
                    }
                }
            }
        }
        return conf;
    }

    /**
     * This is required for the SignChangeEvent, because we have no updated BlockState there.
     * @param state
     */
    public YamlConfiguration serialize(BlockState state, Side side, String[] lines) {
        YamlConfiguration conf = state == null ? null : serialize(state);
        if (lines != null) {
            if (conf == null) {
                conf = new YamlConfiguration();
            }
            ConfigurationSection sideSection = side == Side.FRONT ? conf : conf.getConfigurationSection(side.name().toLowerCase());
            if (sideSection == null) {
                sideSection = conf.createSection(side.name().toLowerCase());
            }
            sideSection.set("lines", Arrays.asList(lines));
        }
        return conf;
    }

    @Override
    public void deserialize(BlockState state, YamlConfiguration conf) {
        if (state instanceof Sign) {
            Sign sign = (Sign) state;
            if (conf != null) {
                sign.setWaxed(conf.getBoolean("waxed"));
                for (Side side : Side.values()) {
                    ConfigurationSection sideSection = side == Side.FRONT ? conf : conf.getConfigurationSection(side.name().toLowerCase());
                    DyeColor signColor = DyeColor.BLACK;
                    boolean glowing = false;
                    List<String> lines = Collections.emptyList();
                    if (sideSection != null) {
                        if (sideSection.contains("lines")) {
                            lines = sideSection.getStringList("lines");
                        }
                        if (sideSection.contains("color")) {
                            try {
                                signColor = DyeColor.valueOf(sideSection.getString("color"));
                            } catch (IllegalArgumentException | NullPointerException e) {
                                // ignored
                            }
                        }
                        glowing = sideSection.getBoolean("glowing", false);
                    }
                    SignSide signSide = sign.getSide(side);
                    for (int i = 0; i < 4; i++) {
                        String line = lines.size() > i && lines.get(i) != null ? lines.get(i) : "";
                        signSide.setLine(i, line);
                    }
                    signSide.setColor(signColor);
                    signSide.setGlowingText(glowing);
                }
            } else {
                sign.setWaxed(false);
                for (Side side : Side.values()) {
                    SignSide signSide = sign.getSide(side);
                    for (int i = 0; i < 4; i++) {
                        signSide.setLine(i, "");
                    }
                    signSide.setColor(DyeColor.BLACK);
                    signSide.setGlowingText(false);
                }
            }
        }
    }

    @Override
    public BaseComponent getChangesAsComponent(YamlConfiguration state, YamlConfiguration oldState) {
        if (state != null) {
            TextComponent tc = new TextComponent();
            // StringBuilder sb = new StringBuilder();
            boolean isWaxed = state.getBoolean("waxed");
            boolean oldWaxed = oldState != null && oldState.getBoolean("waxed");
            if (isWaxed != oldWaxed) {
                tc.addExtra(isWaxed ? "(waxed)" : "(not waxed)");
            }
            for (Side side : Side.values()) {
                ConfigurationSection sideSection = side == Side.FRONT ? state : state.getConfigurationSection(side.name().toLowerCase());
                if (tc.getExtra() != null && !tc.getExtra().isEmpty()) {
                    tc.addExtra(" ");
                }
                tc.addExtra(side.name() + ":");

                List<String> lines = sideSection == null ? Collections.emptyList() : sideSection.getStringList("lines");
                List<String> oldLines = Collections.emptyList();
                DyeColor signColor = DyeColor.BLACK;
                if (sideSection != null && sideSection.contains("color")) {
                    try {
                        signColor = DyeColor.valueOf(sideSection.getString("color"));
                    } catch (IllegalArgumentException | NullPointerException e) {
                        // ignored
                    }
                }
                DyeColor oldSignColor = DyeColor.BLACK;
                boolean glowing = sideSection != null && sideSection.getBoolean("glowing", false);
                boolean oldGlowing = false;
                if (oldState != null) {
                    ConfigurationSection oldSideSection = side == Side.FRONT ? oldState : oldState.getConfigurationSection(side.name().toLowerCase());
                    if (oldSideSection != null) {
                        oldLines = oldSideSection.getStringList("lines");
                        if (oldSideSection.contains("color")) {
                            try {
                                oldSignColor = DyeColor.valueOf(oldSideSection.getString("color"));
                            } catch (IllegalArgumentException | NullPointerException e) {
                                // ignored
                            }
                        }
                        oldGlowing = oldSideSection.getBoolean("glowing", false);
                    }
                }

                if (!lines.equals(oldLines)) {
                    for (String line : lines) {
                        if (tc.getExtra() != null && !tc.getExtra().isEmpty()) {
                            tc.addExtra(" ");
                        }
                        tc.addExtra("[");
                        if (line != null && !line.isEmpty()) {
                            tc.addExtra(new TextComponent(TextComponent.fromLegacyText(line)));
                        }
                        tc.addExtra("]");
                    }
                }
                if (signColor != oldSignColor) {
                    if (tc.getExtra() != null && !tc.getExtra().isEmpty()) {
                        tc.addExtra(" ");
                    }
                    tc.addExtra("(color: ");
                    TextComponent colorText = new TextComponent(signColor.name().toLowerCase());
                    colorText.setColor(ChatColor.of(new Color(signColor.getColor().asARGB())));
                    tc.addExtra(colorText);
                    tc.addExtra(")");
                }
                if (glowing != oldGlowing) {
                    if (tc.getExtra() != null && !tc.getExtra().isEmpty()) {
                        tc.addExtra(" ");
                    }
                    if (glowing) {
                        tc.addExtra("(glowing)");
                    } else {
                        tc.addExtra("(not glowing)");
                    }
                }
            }
            return tc;
        }
        return null;
    }
}
