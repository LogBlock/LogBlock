package de.diddiz.LogBlock.util;

public class SqlUtil {
    public static String escapeString(String s) {
        return escapeString(s, false);
    }

    public static String escapeString(String s, boolean escapeMatcher) {
        s = s.replace("\u0000", "\\0");
        s = s.replace("\u0026", "\\Z");
        s = s.replace("\\", "\\\\");
        s = s.replace("'", "\\'");
        s = s.replace("\"", "\\\"");
        s = s.replace("\b", "\\b");
        s = s.replace("\n", "\\n");
        s = s.replace("\r", "\\r");
        s = s.replace("\t", "\\t");
        if (escapeMatcher) {
            s = s.replace("%", "\\%");
            s = s.replace("_", "\\_");
        }
        return s;
    }
}
