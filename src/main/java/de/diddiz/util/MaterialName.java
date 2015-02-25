package de.diddiz.util;

import static de.diddiz.util.Utils.isInt;
import static de.diddiz.util.Utils.isShort;
import static org.bukkit.Bukkit.getLogger;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import net.milkbowl.vault.item.ItemInfo;
import net.milkbowl.vault.item.Items;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.material.MaterialData;

import de.diddiz.LogBlock.LogBlock;

public class MaterialName
{
	private static final String[] COLORS = {"white","orange","magenta","light blue","yellow","lime","pink","gray","silver","cyan","purple","blue","brown","green","red","black"};
	private static final Map<Integer, String> materialNames = new HashMap<Integer, String>();
	private static final Map<Integer, Map<Short, String>> materialDataNames = new HashMap<Integer, Map<Short, String>>();

	static {
		// Add all known materials
		for (final Material mat : Material.values())
			materialNames.put(mat.getId(), mat.toString().replace('_', ' ').toLowerCase());
		if(LogBlock.isVaultInstalled()){
			for (ItemInfo item : Items.getItemList()) {
				if (item.getSubTypeId() == 0){
					materialNames.put(item.getId(), item.getName());
				}
				Map<Short, String> dataNames = new HashMap<Short, String>();
				if (materialDataNames.containsKey(item.getId())) {
					//rescue datavalues set before!
					dataNames = materialDataNames.get(item.getId());
				}
				dataNames.put(item.subTypeId, item.getName());
				materialDataNames.put(item.getId(), dataNames);

			}
		}
		// Load config
		final File file = new File("plugins/LogBlock/materials.yml");
		final YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
		if (cfg.getKeys(false).isEmpty()) {
			// Generate defaults
			cfg.options().header("Add block or item names you want to be overridden or also names for custom blocks");
			cfg.set("1.1", "granite");
			cfg.set("1.2", "polished granite");
			cfg.set("1.3", "diorite");
			cfg.set("1.4", "polished diorite");
			cfg.set("1.5", "andesite");
			cfg.set("1.6", "polished andesite");
			cfg.set("5.0", "oak wood");
			cfg.set("5.1", "spruce wood");
			cfg.set("5.2", "birch wood");
			cfg.set("5.3", "jungle wood");
			cfg.set("5.4", "acacia wood");
			cfg.set("5.5", "dark oak wood");
			cfg.set("3.1", "coarse dirt");
			cfg.set("3.2", "podzol");
			cfg.set("6.1", "redwood sapling");
			cfg.set("6.2", "birch sapling");
			cfg.set("6.3", "jungle sapling");
			cfg.set("6.4", "acacia sapling");
			cfg.set("6.5", "dark oak sapling");
			cfg.set("9", "water");
			cfg.set("11", "lava");
			cfg.set("12.1", "red sand");
			cfg.set("17.0", "oak log");
			cfg.set("17.1", "spruce log");
			cfg.set("17.2", "birch log");
			cfg.set("17.3", "jungle log");
			cfg.set("17.4", "oak log");
			cfg.set("17.5", "spruce log");
			cfg.set("17.6", "birch log");
			cfg.set("17.7", "jungle log");
			cfg.set("17.8", "oak log");
			cfg.set("17.9", "spruce log");
			cfg.set("17.10", "birch log");
			cfg.set("17.11", "jungle log");
			cfg.set("17.12", "oak log");
			cfg.set("17.13", "spruce log");
			cfg.set("17.14", "birch log");
			cfg.set("17.15", "jungle log");
			cfg.set("18.1", "spruce leaves");
			cfg.set("18.2", "birch leaves");
			cfg.set("18.3", "jungle leaves");
			cfg.set("18.4", "oak leaves");
			cfg.set("18.5", "spruce leaves");
			cfg.set("18.6", "birch leaves");
			cfg.set("18.7", "jungle leaves");
			cfg.set("18.8", "oak leaves");
			cfg.set("18.9", "spruce leaves");
			cfg.set("18.10", "birch leaves");
			cfg.set("18.11", "jungle leaves");
			cfg.set("18.12", "oak leaves");
			cfg.set("18.13", "spruce leaves");
			cfg.set("18.14", "birch leaves");
			cfg.set("18.15", "jungle leaves");
			cfg.set("19.1", "wet sponge");
			cfg.set("37.0", "dandelion");
			cfg.set("38.0", "poppy");
			cfg.set("38.1", "blue orchid");
			cfg.set("38.2", "allium");
			cfg.set("38.3", "azure bluet");
			cfg.set("38.4", "red tulip");
			cfg.set("38.5", "orange tulip");
			cfg.set("38.6", "white tulip");
			cfg.set("38.7", "pink tulip");
			cfg.set("38.8", "oxeye daisy");
			cfg.set("24.1", "chiseled sandstone");
			cfg.set("24.2", "smooth sandstone");
			cfg.set("31.0", "dead bush");
			cfg.set("31.1", "tall grass");
			cfg.set("31.2", "fern");
			cfg.set("98.0", "stone brick");
			cfg.set("98.1", "mossy stone brick");
			cfg.set("98.2", "cracked stone brick");
			cfg.set("98.3", "chiseled stone brick");
			cfg.set("125.0","oak double step");
			cfg.set("125.1","spruce double step");
			cfg.set("125.2","birch double step");
			cfg.set("125.3","jungle double step");
			cfg.set("125.4","acacia double step");
			cfg.set("125.5","dark oak double step");
			cfg.set("126.0", "oak step");
			cfg.set("126.1", "spruce step");
			cfg.set("126.2", "birch step");
			cfg.set("126.3", "jungle step");
			cfg.set("126.4", "acacia step");
			cfg.set("126.5", "dark oak step");
			cfg.set("126.8", "oak step");
			cfg.set("126.9", "spruce step");
			cfg.set("126.10", "birch step");
			cfg.set("126.11", "jungle step");
			cfg.set("126.12", "acacia step");
			cfg.set("126.13", "dark oak step");
			cfg.set("139.1", "mossy cobble wall");
			cfg.set("155.1", "chiseled quartz block");
			cfg.set("155.2", "pillar quartz block");
			cfg.set("155.3", "pillar quartz block");
			cfg.set("155.4", "pillar quartz block");
			cfg.set("161.0", "acacia leaves");
			cfg.set("161.1", "dark oak leaves");
			cfg.set("161.4", "acacia leaves");
			cfg.set("161.5", "dark oak leaves");
			cfg.set("161.8", "acacia leaves");
			cfg.set("161.9", "dark oak leaves");
			cfg.set("161.12", "acacia leaves");
			cfg.set("161.13", "dark oak leaves");
			cfg.set("162.0", "acacia log");
			cfg.set("162.1", "dark oak log");
			cfg.set("162.4", "acacia log");
			cfg.set("162.5", "dark oak log");
			cfg.set("162.8", "acacia log");
			cfg.set("162.9", "dark oak log");
			cfg.set("162.12", "acacia log");
			cfg.set("162.13", "dark oak log");
			cfg.set("168.1", "prismarine bricks");
			cfg.set("168.2", "dark prismarine");
			cfg.set("181.0", "red sandstone double step");
			cfg.set("181.8", "smooth red sandstone double step");
			cfg.set("162.13", "dark oak log");
			cfg.set("175.0", "sunflower");
			cfg.set("175.1", "lilac");
			cfg.set("175.2", "double tall grass");
			cfg.set("175.3", "large fern");
			cfg.set("175.4", "rose bush");
			cfg.set("175.5", "peony");
			cfg.set("175.8", "sunflower");
			cfg.set("175.9", "lilac");
			cfg.set("175.10", "double tall grass");
			cfg.set("175.11", "large fern");
			cfg.set("175.12", "rose bush");
			cfg.set("175.13", "peony");
			cfg.set("179.1", "chiseled sandstone");
			cfg.set("179.2", "smooth sandstone");
			cfg.set("263.1", "charcoal");
			for (byte i = 0; i < 10; i++) {
				cfg.set("43." + i, toReadable(Material.DOUBLE_STEP.getNewData(i)));
			}
			cfg.set("43.8", "stone double step");
			cfg.set("43.9", "sandstone double step");
			cfg.set("43.15", "quartz double step");
			for (byte i = 0; i < 8; i++) {
				cfg.set("44." + i, toReadable(Material.STEP.getNewData(i)));
				// The second half of this data list should read the same as the first half
				cfg.set("44." + (i+7), toReadable(Material.STEP.getNewData(i)));
			}
			for (byte i = 0; i < 16; i++) {
				cfg.set("351." + i, toReadable(Material.INK_SACK.getNewData(i)));
				cfg.set("35." + i, COLORS[i] + " wool");
				cfg.set("159." + i, COLORS[i] + " stained clay");
				cfg.set("95." + i, COLORS[i] + " stained glass");
				cfg.set("160." + i,  COLORS[i] + " stained glass pane");
				cfg.set("171." + i,  COLORS[i] + " carpet");
			}
			for (byte i = 0; i < 6; i++) {
				cfg.set("125." + i, toReadable(Material.WOOD_DOUBLE_STEP.getNewData(i)));
				cfg.set("126." + i, toReadable(Material.WOOD_STEP.getNewData(i)));
				cfg.set("126." + i+8, toReadable(Material.WOOD_STEP.getNewData(i)));
			}
			try {
				cfg.save(file);
			} catch (final IOException ex) {
				getLogger().log(Level.WARNING, "Unable to save material.yml: ", ex);
			}
		}
		if (cfg.getString("263.1") == null) {
			getLogger().info("[Logblock-names] Logblock's default materials.yml file has been updated with more names");
			getLogger().info("[Logblock-names] Consider deleting your current materials.yml file to allow it to be recreated");
		}
		for (final String entry : cfg.getKeys(false))
			if (isInt(entry)) {
				if (cfg.isString(entry))
					materialNames.put(Integer.valueOf(entry), cfg.getString(entry));
				else if (cfg.isConfigurationSection(entry)) {
					Map<Short, String> dataNames = new HashMap<Short, String>();
					if (materialDataNames.containsKey(Integer.valueOf(entry))) {
						//rescue datavalues set before!
						dataNames = materialDataNames.get(Integer.valueOf(entry));
					}
					materialDataNames.put(Integer.valueOf(entry), dataNames);
					final ConfigurationSection sec = cfg.getConfigurationSection(entry);
					for (final String data : sec.getKeys(false))
						if (isShort(data)) {
							if (sec.isString(data))
								dataNames.put(Short.valueOf(data), sec.getString(data));
							else
								getLogger().warning("Parsing materials.yml: '" + data + "' is not a string.");
						} else
							getLogger().warning("Parsing materials.yml: '" + data + "' is no valid material data");
				} else
					getLogger().warning("Parsing materials.yml: '" + entry + "' is neither a string nor a section.");
			} else
				getLogger().warning("Parsing materials.yml: '" + entry + "' is no valid material id");
	}

	/**
	 * @return Name of the material, or if it's unknown, the id.
	 */
	public static String materialName(int type) {
		return materialNames.containsKey(type) ? materialNames.get(type) : String.valueOf(type);
	}

	/**
	 * @return Name of the material regarding it's data, or if it's unknown, the basic name.
	 */
	public static String materialName(int type, short data) {
		final Map<Short, String> dataNames = materialDataNames.get(type);
		if (dataNames != null)
			if (dataNames.containsKey(data))
				return dataNames.get(data);
		return materialName(type);
	}

	private static String toReadable(MaterialData matData) {
		return matData.toString().toLowerCase().replace('_', ' ').replaceAll("[^a-z ]", "");
	}
	
}
