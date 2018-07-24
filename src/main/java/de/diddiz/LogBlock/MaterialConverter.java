package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.BlockData;

public class MaterialConverter {
    private static String[] idToMaterial = new String[10];
    private static HashMap<String, Integer> materialToID = new HashMap<>();
    private static int nextMaterialId;

    private static String[] idToBlockState = new String[10];
    private static HashMap<String, Integer> blockStateToID = new HashMap<>();
    private static int nextBlockStateId;
    
    private static HashMap<String, Material> materialKeyToMaterial = new HashMap<>();

    static {
        for (Material m : Material.values()) {
            if (!m.name().startsWith("LEGACY_") && m.getKey() != null) {
                materialKeyToMaterial.put(m.getKey().toString(), m);
            }
        }
    }

    public static int getOrAddMaterialId(NamespacedKey nameSpaceKey) {
        return getOrAddMaterialId(nameSpaceKey.toString());
    }

    public static int getOrAddMaterialId(String blockDataString) {
        String materialString = blockDataString;
        int dataPart = blockDataString.indexOf("[");
        if (dataPart >= 0) {
            materialString = blockDataString.substring(0, dataPart);
        }
        Integer key = materialToID.get(materialString);
        if (key == null) {
            key = nextMaterialId++;
            materialToID.put(materialString, key);
            int length = idToMaterial.length;
            while (length <= key) {
                length = (length * 3 / 2) + 5;
            }
            if (length > idToMaterial.length) {
                idToMaterial = Arrays.copyOf(idToMaterial, length);
            }
            idToMaterial[key] = materialString;
            LogBlock.getInstance().getConsumer().queueAddMaterialMapping(key, materialString);
        }
        return key.intValue();
    }

    public static int getOrAddBlockStateId(String blockDataString) {
        int dataPart = blockDataString.indexOf("[");
        if (dataPart < 0) {
            return -1;
        }
        String materialString = blockDataString.substring(dataPart);
        Integer key = blockStateToID.get(materialString);
        if (key == null) {
            key = nextBlockStateId++;
            blockStateToID.put(materialString, key);
            int length = idToBlockState.length;
            while (length <= key) {
                length = (length * 3 / 2) + 5;
            }
            if (length > idToBlockState.length) {
                idToBlockState = Arrays.copyOf(idToBlockState, length);
            }
            idToBlockState[key] = materialString;
            LogBlock.getInstance().getConsumer().queueAddBlockStateMapping(key, materialString);
        }
        return key.intValue();
    }

    public static BlockData getBlockData(int materialId, int blockStateId) {
        String material = idToMaterial[materialId];
        if (blockStateId >= 0) {
            material = material + idToBlockState[blockStateId];
        }
        return Bukkit.createBlockData(material);
    }

    public static Material getMaterial(int materialId) {
        return materialKeyToMaterial.get(idToMaterial[materialId]);
    }

    public static void initializeMaterials(Connection connection) throws SQLException {
        Statement smt = connection.createStatement();
        ResultSet rs = smt.executeQuery("SELECT id, name FROM `lb-materials`");
        while (rs.next()) {
            int key = rs.getInt(1);
            String materialString = rs.getString(2);

            materialToID.put(materialString, key);
            int length = idToMaterial.length;
            while (length <= key) {
                length = (length * 3 / 2) + 5;
            }
            if (length > idToMaterial.length) {
                idToMaterial = Arrays.copyOf(idToMaterial, length);
            }
            idToMaterial[key] = materialString;
            if (nextMaterialId <= key) {
                nextMaterialId = key + 1;
            }
        }
        rs.close();
        rs = smt.executeQuery("SELECT id, name FROM `lb-blockstates`");
        while (rs.next()) {
            int key = rs.getInt(1);
            String materialString = rs.getString(2);

            blockStateToID.put(materialString, key);
            int length = idToBlockState.length;
            while (length <= key) {
                length = (length * 3 / 2) + 5;
            }
            if (length > idToBlockState.length) {
                idToBlockState = Arrays.copyOf(idToBlockState, length);
            }
            idToBlockState[key] = materialString;
            if (nextBlockStateId <= key) {
                nextBlockStateId = key + 1;
            }
        }
        rs.close();
        smt.close();
        connection.close();
    }
}
