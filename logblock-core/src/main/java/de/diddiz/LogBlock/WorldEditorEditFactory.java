package de.diddiz.LogBlock;

import java.sql.ResultSet;
import java.sql.SQLException;

import de.diddiz.LogBlock.QueryParams.BlockChangeType;
import de.diddiz.LogBlock.util.ItemStackAndAmount;
import de.diddiz.LogBlock.util.Utils;

public class WorldEditorEditFactory {
    private final WorldEditor editor;
    private final boolean rollback;
    private final QueryParams params;

    public WorldEditorEditFactory(WorldEditor editor, QueryParams params, boolean rollback) {
        this.editor = editor;
        this.params = params;
        this.rollback = rollback;
    }

    public void processRow(ResultSet rs) throws SQLException {
        if (params.bct == BlockChangeType.ENTITIES) {
            editor.queueEntityEdit(rs, params, rollback);
            return;
        }
        ChestAccess chestaccess = null;
        ItemStackAndAmount stack = Utils.loadItemStack(rs.getBytes("item"));
        if (stack != null) {
            chestaccess = new ChestAccess(stack, rs.getBoolean("itemremove") == rollback, rs.getInt("itemtype"));
        }
        if (rollback) {
            editor.queueBlockEdit(rs.getTimestamp("date").getTime(), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), rs.getInt("replaced"), rs.getInt("replacedData"), rs.getBytes("replacedState"), rs.getInt("type"), rs.getInt("typeData"), rs.getBytes("typeState"), chestaccess);
        } else {
            editor.queueBlockEdit(rs.getTimestamp("date").getTime(), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), rs.getInt("type"), rs.getInt("typeData"), rs.getBytes("typeState"), rs.getInt("replaced"), rs.getInt("replacedData"), rs.getBytes("replacedState"), chestaccess);
        }
    }
}
