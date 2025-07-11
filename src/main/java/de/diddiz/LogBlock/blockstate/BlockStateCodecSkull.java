package de.diddiz.LogBlock.blockstate;

import de.diddiz.LogBlock.componentwrapper.Component;
import de.diddiz.LogBlock.componentwrapper.Components;
import de.diddiz.LogBlock.componentwrapper.Hover;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.profile.PlayerProfile;

public class BlockStateCodecSkull implements BlockStateCodec {
    private static final boolean HAS_PROFILE_API;
    static {
        boolean hasProfileApi = false;
        try {
            Skull.class.getMethod("getOwnerProfile");
            hasProfileApi = true;
        } catch (NoSuchMethodException ignored) {
        }
        HAS_PROFILE_API = hasProfileApi;
    }

    @Override
    public Material[] getApplicableMaterials() {
        return new Material[] { Material.PLAYER_WALL_HEAD, Material.PLAYER_HEAD };
    }

    @Override
    public YamlConfiguration serialize(BlockState state) {
        if (state instanceof Skull) {
            Skull skull = (Skull) state;
            OfflinePlayer owner = skull.hasOwner() ? skull.getOwningPlayer() : null;
            PlayerProfile profile = HAS_PROFILE_API ? skull.getOwnerProfile() : null;
            if (owner != null || profile != null) {
                YamlConfiguration conf = new YamlConfiguration();
                if (profile != null) {
                    conf.set("profile", profile);
                } else if (owner != null) {
                    conf.set("owner", owner.getUniqueId().toString());
                }
                return conf;
            }
        }
        return null;
    }

    @Override
    public void deserialize(BlockState state, YamlConfiguration conf) {
        if (state instanceof Skull) {
            Skull skull = (Skull) state;
            PlayerProfile profile = conf == null || !HAS_PROFILE_API ? null : (PlayerProfile) conf.get("profile");
            if (profile != null) {
                skull.setOwnerProfile(profile);
            } else {
                UUID ownerId = conf == null ? null : UUID.fromString(conf.getString("owner"));
                if (ownerId == null) {
                    skull.setOwningPlayer(null);
                } else {
                    skull.setOwningPlayer(Bukkit.getOfflinePlayer(ownerId));
                }
            }
        }
    }

    @Override
    public Component getChangesAsComponent(YamlConfiguration conf, YamlConfiguration oldState) {
        if (HAS_PROFILE_API && conf != null) {
            PlayerProfile profile = (PlayerProfile) conf.get("profile");
            if (profile != null) {
                Component tc = Components.text("[" + (profile.getName() != null ? profile.getName() : (profile.getUniqueId() != null ? profile.getUniqueId().toString() : "~unknown~")) + "]");
                if (profile.getName() != null && profile.getUniqueId() != null) {
                    tc = tc.hover(Hover.text("UUID: " + profile.getUniqueId().toString()));
                }
                return tc;
            }
        }
        String ownerIdString = conf == null ? null : conf.getString("owner");
        UUID ownerId = ownerIdString == null ? null : UUID.fromString(ownerIdString);
        if (ownerId != null) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
            return Components.text("[" + (owner.getName() != null ? owner.getName() : owner.getUniqueId().toString()) + "]");
        }
        return null;
    }
}
