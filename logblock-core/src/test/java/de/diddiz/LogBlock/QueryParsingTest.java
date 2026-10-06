package de.diddiz.LogBlock;

import org.junit.Assert;
import org.junit.Test;
import de.diddiz.LogBlock.util.Utils;
import java.util.Arrays;
import java.util.List;

public class QueryParsingTest {

    @Test
    public void testParseQuotes() {
        // input = /lb clearlog world "my world of swag" player "player"
        List<String> input = Arrays.asList("lb", "clearlog", "world", "\"my", "world", "of", "swag\"", "player", "\"player\"");
        List<String> expectedOut = Arrays.asList("lb", "clearlog", "world", "\"my world of swag\"", "player", "\"player\"");
        Assert.assertEquals(Utils.parseQuotes(input), expectedOut);
    }

}
