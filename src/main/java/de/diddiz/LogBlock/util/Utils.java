package de.diddiz.LogBlock.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipException;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import de.diddiz.LogBlock.LogBlock;

public class Utils {
    public static String newline = System.getProperty("line.separator");

    public static boolean isInt(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (final NumberFormatException ex) {
        }
        return false;
    }

    public static boolean isShort(String str) {
        try {
            Short.parseShort(str);
            return true;
        } catch (final NumberFormatException ex) {
        }
        return false;
    }

    public static boolean isByte(String str) {
        try {
            Byte.parseByte(str);
            return true;
        } catch (final NumberFormatException ex) {
        }
        return false;
    }

    public static String listing(String[] entries, String delimiter, String finalDelimiter) {
        final int len = entries.length;
        if (len == 0) {
            return "";
        }
        if (len == 1) {
            return entries[0];
        }
        final StringBuilder builder = new StringBuilder(entries[0]);
        for (int i = 1; i < len - 1; i++) {
            builder.append(delimiter).append(entries[i]);
        }
        builder.append(finalDelimiter).append(entries[len - 1]);
        return builder.toString();
    }

    public static String listing(List<?> entries, String delimiter, String finalDelimiter) {
        final int len = entries.size();
        if (len == 0) {
            return "";
        }
        if (len == 1) {
            return entries.get(0).toString();
        }
        final StringBuilder builder = new StringBuilder(entries.get(0).toString());
        for (int i = 1; i < len - 1; i++) {
            builder.append(delimiter).append(entries.get(i).toString());
        }
        builder.append(finalDelimiter).append(entries.get(len - 1).toString());
        return builder.toString();
    }

    public static int parseTimeSpec(String[] spec) {
        if (spec == null || spec.length < 1 || spec.length > 2) {
            return -1;
        }
        if (spec.length == 1 && isInt(spec[0])) {
            return Integer.valueOf(spec[0]);
        }
        if (!spec[0].contains(":") && !spec[0].contains(".")) {
            if (spec.length == 2) {
                if (!isInt(spec[0])) {
                    return -1;
                }
                int min = Integer.parseInt(spec[0]);
                if (spec[1].startsWith("h")) {
                    min *= 60;
                } else if (spec[1].startsWith("d")) {
                    min *= 1440;
                }
                return min;
            } else if (spec.length == 1) {
                int days = 0, hours = 0, minutes = 0;
                int lastIndex = 0, currIndex = 1;
                while (currIndex <= spec[0].length()) {
                    while (currIndex <= spec[0].length() && isInt(spec[0].substring(lastIndex, currIndex))) {
                        currIndex++;
                    }
                    if (currIndex - 1 != lastIndex) {
                        if (currIndex > spec[0].length()) {
                            return -1;
                        }
                        final String param = spec[0].substring(currIndex - 1, currIndex).toLowerCase();
                        if (param.equals("d")) {
                            days = Integer.parseInt(spec[0].substring(lastIndex, currIndex - 1));
                        } else if (param.equals("h")) {
                            hours = Integer.parseInt(spec[0].substring(lastIndex, currIndex - 1));
                        } else if (param.equals("m")) {
                            minutes = Integer.parseInt(spec[0].substring(lastIndex, currIndex - 1));
                        }
                    }
                    lastIndex = currIndex;
                    currIndex++;
                }
                if (days == 0 && hours == 0 && minutes == 0) {
                    return -1;
                }
                return minutes + hours * 60 + days * 1440;
            } else {
                return -1;
            }
        }
        final String timestamp;
        if (spec.length == 1) {
            if (spec[0].contains(":")) {
                timestamp = new SimpleDateFormat("dd.MM.yyyy").format(System.currentTimeMillis()) + " " + spec[0];
            } else {
                timestamp = spec[0] + " 00:00:00";
            }
        } else {
            timestamp = spec[0] + " " + spec[1];
        }
        try {
            return (int) ((System.currentTimeMillis() - new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").parse(timestamp).getTime()) / 60000);
        } catch (final ParseException ex) {
            return -1;
        }
    }

    public static String spaces(int count) {
        final StringBuilder filled = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            filled.append(' ');
        }
        return filled.toString();
    }

    public static String join(String[] s, String delimiter) {
        if (s == null || s.length == 0) {
            return "";
        }
        final int len = s.length;
        final StringBuilder builder = new StringBuilder(s[0]);
        for (int i = 1; i < len; i++) {
            builder.append(delimiter).append(s[i]);
        }
        return builder.toString();
    }

    /**
     * Converts a list of arguments e.g ['lb', 'clearlog', 'world', '"my', 'world', 'of', 'swag"']
     * into a list of arguments with any text encapsulated by quotes treated as one word
     * For this particular example: ['lb', 'clearlog', 'world', '"my world of swag"']
     *
     * @param args The list of arguments
     * @return A new list with the quoted arguments parsed to single values
     */
    public static List<String> parseQuotes(List<String> args) {
        List<String> newArguments = new ArrayList<>();
        String subjectString = join(args.toArray(new String[args.size()]), " ");

        Pattern regex = Pattern.compile("[^\\s\"']+|\"[^\"]*\"|'[^']*'");
        Matcher regexMatcher = regex.matcher(subjectString);
        while (regexMatcher.find()) {
            newArguments.add(regexMatcher.group());
        }

        return newArguments;
    }

    public static class ExtensionFilenameFilter implements FilenameFilter {
        private final String ext;

        public ExtensionFilenameFilter(String ext) {
            this.ext = "." + ext;
        }

        @Override
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(ext);
        }
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String mysqlEscapeBytes(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2 + 2];
        hexChars[0] = '0';
        hexChars[1] = 'x';
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2 + 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 3] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String mysqlPrepareBytesForInsertAllowNull(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }
        return "'" + mysqlEscapeBytes(bytes) + "'";
    }

    public static String mysqlTextEscape(String untrusted) {
        return untrusted.replace("\\", "\\\\").replace("'", "\\'");
    }

    public static ItemStackAndAmount loadItemStack(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        YamlConfiguration conf = deserializeYamlConfiguration(data);
        if (conf == null) {
            return null;
        }
        ItemStack stack = conf.getItemStack("stack");
        if (stack == null) {
            return null;
        }
        int amount = conf.contains("amount") ? conf.getInt("amount") : stack.getAmount();
        stack.setAmount(1);
        return new ItemStackAndAmount(stack, amount);
    }

    public static byte[] saveItemStack(ItemStackAndAmount stack) {
        if (stack == null || stack.stack() == null || BukkitUtils.isEmpty(stack.stack().getType())) {
            return null;
        }
        YamlConfiguration conf = new YamlConfiguration();
        ItemStack itemStack = stack.stack();
        if (itemStack.getAmount() > 1) {
            itemStack = itemStack.clone();
            itemStack.setAmount(1);
        }
        conf.set("stack", itemStack);
        conf.set("amount", stack.amount());
        return serializeYamlConfiguration(conf);
    }

    public static YamlConfiguration deserializeYamlConfiguration(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        YamlConfiguration conf = new YamlConfiguration();
        try {
            InputStreamReader reader = new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(data)), "UTF-8");
            conf.load(reader);
            reader.close();
            return conf;
        } catch (ZipException | InvalidConfigurationException e) {
            LogBlock.getInstance().getLogger().warning("Could not deserialize YamlConfiguration: " + e.getMessage());
            return conf;
        } catch (IOException e) {
            throw new RuntimeException("IOException should be impossible for ByteArrayInputStream", e);
        }
    }

    public static byte[] serializeYamlConfiguration(YamlConfiguration conf) {
        if (conf == null || conf.getKeys(false).isEmpty()) {
            return null;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(new GZIPOutputStream(baos), "UTF-8");
            writer.write(conf.saveToString());
            writer.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("IOException should be impossible for ByteArrayOutputStream", e);
        }
    }

    public static String serializeForSQL(YamlConfiguration conf) {
        return mysqlPrepareBytesForInsertAllowNull(serializeYamlConfiguration(conf));
    }

    public static double warpDegrees(double degrees) {
        double d = degrees % 360.0;
        if (d >= 180.0) {
            d -= 360.0;
        }
        if (d < -180.0) {
            d += 360.0;
        }
        return d;
    }
}
