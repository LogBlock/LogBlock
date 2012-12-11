package de.diddiz.LogBlock;

import java.sql.ResultSet;
import java.sql.SQLException;
import de.diddiz.LogBlock.QueryParams.BlockChangeType;
import de.diddiz.LogBlock.QueryParams.SummarizationMode;

public class LookupCacheElementFactory
{
	private final QueryParams params;
	private final float spaceFactor;

	public LookupCacheElementFactory(QueryParams params, float spaceFactor) {
		this.params = params;
		this.spaceFactor = spaceFactor;
	}

	public LookupCacheElement getLookupCacheElement(ResultSet rs) throws SQLException {
		if (params.bct == BlockChangeType.CHAT)
			return new ChatMessage(rs, params);
		if (params.bct == BlockChangeType.KILLS)
			if (params.sum == SummarizationMode.NONE)
				return new Kill(rs, params);
			else if (params.sum == SummarizationMode.PLAYERS)
				return new SummedKills(rs, params, spaceFactor);
		if (params.sum == SummarizationMode.NONE)
			return new BlockChange(rs, params);
		return new SummedBlockChanges(rs, params, spaceFactor);
	}
}
