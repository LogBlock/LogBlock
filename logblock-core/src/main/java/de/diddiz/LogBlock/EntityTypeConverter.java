package de.diddiz.LogBlock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.entity.EntityType;

public class EntityTypeConverter {
    private static EntityType[] idToEntityType = new EntityType[10];
    private static HashMap<EntityType, Integer> entityTypeToId = new HashMap<>();
    private static int nextEntityTypeId;

    public synchronized static Integer getExistingEntityTypeId(EntityType entityType) {
        return entityType == null ? null : entityTypeToId.get(entityType);
    }

    public synchronized static int getOrAddEntityTypeId(EntityType entityType) {
        Integer key = entityTypeToId.get(entityType);
        int tries = 0;
        while (key == null && tries < 10) {
            tries++;
            key = nextEntityTypeId;
            Connection conn = LogBlock.getInstance().getConnection();
            try {
                conn.setAutoCommit(false);
                PreparedStatement smt = conn.prepareStatement("INSERT IGNORE INTO `lb-entitytypes` (id, name) VALUES (?, ?)");
                smt.setInt(1, key);
                smt.setString(2, entityType.name());
                boolean couldAdd = smt.executeUpdate() > 0;
                conn.commit();
                smt.close();
                if (couldAdd) {
                    internalAddEntityType(key, entityType);
                } else {
                    initializeEntityTypes(conn);
                }
            } catch (Exception e) {
                LogBlock.getInstance().getLogger().log(Level.SEVERE, "Could not update lb-entitytypes", e);
                reinitializeEntityTypesCatchException();
                if (tries == 10) {
                    throw new RuntimeException(e);
                }
            } finally {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // ignored
                }
            }
            key = entityTypeToId.get(entityType);
        }
        return key.intValue();
    }

    public synchronized static EntityType getEntityType(int entityTypeId) {
        return entityTypeId >= 0 && entityTypeId < idToEntityType.length ? idToEntityType[entityTypeId] : null;
    }

    private static void reinitializeEntityTypesCatchException() {
        Connection conn = LogBlock.getInstance().getConnection();
        try {
            initializeEntityTypes(conn);
        } catch (Exception e) {
            LogBlock.getInstance().getLogger().log(Level.SEVERE, "Could not reinitialize lb-entitytypes", e);
        } finally {
            try {
                conn.close();
            } catch (Exception e) {
                // ignored
            }
        }
    }

    protected synchronized static void initializeEntityTypes(Connection connection) throws SQLException {
        Statement smt = connection.createStatement();
        ResultSet rs = smt.executeQuery("SELECT id, name FROM `lb-entitytypes`");
        while (rs.next()) {
            int key = rs.getInt(1);
            try {
                EntityType entityType = EntityType.valueOf(rs.getString(2));
                internalAddEntityType(key, entityType);
            } catch (IllegalArgumentException ignored) {
                // the key is used, but not available in this version
                if (nextEntityTypeId <= key) {
                    nextEntityTypeId = key + 1;
                }
            }
        }
        rs.close();
        smt.close();
        connection.close();
    }

    private static void internalAddEntityType(int key, EntityType entityType) {
        entityTypeToId.put(entityType, key);
        int length = idToEntityType.length;
        while (length <= key) {
            length = (length * 3 / 2) + 5;
        }
        if (length > idToEntityType.length) {
            idToEntityType = Arrays.copyOf(idToEntityType, length);
        }
        idToEntityType[key] = entityType;
        if (nextEntityTypeId <= key) {
            nextEntityTypeId = key + 1;
        }
    }
}
