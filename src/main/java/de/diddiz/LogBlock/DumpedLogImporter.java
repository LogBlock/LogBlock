package de.diddiz.LogBlock;

import static de.diddiz.LogBlock.util.Utils.newline;

import de.diddiz.LogBlock.util.ExtensionFilenameFilter;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class DumpedLogImporter implements Runnable {
    private final LogBlock logblock;

    DumpedLogImporter(LogBlock logblock) {
        this.logblock = logblock;
    }

    @Override
    public void run() {
        final File[] imports = new File(logblock.getDataFolder(), "import").listFiles(new ExtensionFilenameFilter("sql"));
        if (imports != null && imports.length > 0) {
            logblock.getLogger().info("Found " + imports.length + " imports.");
            Arrays.sort(imports, new ImportsComparator());
            Connection conn = null;
            try {
                conn = logblock.getConnection();
                if (conn == null) {
                    return;
                }
                conn.setAutoCommit(false);
                final Statement st = conn.createStatement();
                final BufferedWriter writer = new BufferedWriter(new FileWriter(new File(logblock.getDataFolder(), "import/failed.txt")));
                int successes = 0, errors = 0;
                try {
                    for (final File sqlFile : imports) {
                        String line = null;
                        try {
                            logblock.getLogger().info("Trying to import " + sqlFile.getName() + " ...");
                            // first try batch import the whole file
                            final BufferedReader reader = new BufferedReader(new FileReader(sqlFile));
                            int statements = 0;
                            while ((line = reader.readLine()) != null) {
                                if (line.endsWith(";")) {
                                    line = line.substring(0, line.length() - 1);
                                }
                                if (!line.isEmpty()) {
                                    statements++;
                                    st.addBatch(line);
                                }
                            }
                            st.executeBatch();
                            conn.commit();
                            reader.close();
                            sqlFile.delete();
                            successes += statements;
                            logblock.getLogger().info("Successfully imported " + sqlFile.getName() + ".");
                        } catch (final Exception ignored) {
                            // if the batch import did not work, retry line by line
                            try {
                                final BufferedReader reader = new BufferedReader(new FileReader(sqlFile));
                                while ((line = reader.readLine()) != null) {
                                    if (line.endsWith(";")) {
                                        line = line.substring(0, line.length() - 1);
                                    }
                                    if (!line.isEmpty()) {
                                        try {
                                            st.execute(line);
                                            successes++;
                                        } catch (final SQLException ex) {
                                            logblock.getLogger().severe("Error while importing: '" + line + "': " + ex.getMessage());
                                            writer.write(line + newline);
                                            errors++;
                                        }
                                    }
                                }
                                conn.commit();
                                reader.close();
                                sqlFile.delete();
                                logblock.getLogger().info("Successfully imported " + sqlFile.getName() + ".");
                            } catch (final Exception ex) {
                                logblock.getLogger().severe("Error while importing " + sqlFile.getName() + ": " + ex.getMessage());
                                errors++;
                            }
                        }
                    }
                } finally {
                    writer.close();
                }
                st.close();
                logblock.getLogger().info("Successfully imported stored queue. (" + successes + " rows imported, " + errors + " errors)");
            } catch (final Exception ex) {
                logblock.getLogger().log(Level.WARNING, "Error while importing: ", ex);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (final SQLException ex) {
                    }
                }
            }
        }
    }

    private static class ImportsComparator implements Comparator<File> {
        private final Pattern splitPattern = Pattern.compile("[\\-\\.]");

        @Override
        public int compare(File o1, File o2) {
            String[] name1 = splitPattern.split(o1.getName());
            String[] name2 = splitPattern.split(o2.getName());
            if (name1.length > name2.length) {
                return 1;
            } else if (name1.length < name2.length) {
                return -1;
            }
            for (int i = 0; i < name1.length; i++) {
                String part1 = name1[i];
                String part2 = name2[i];
                if (part1.length() > 0 && part2.length() > 0) {
                    char first1 = part1.charAt(0);
                    char first2 = part2.charAt(0);
                    if (first1 >= '0' && first1 <= '9' && first2 >= '0' && first2 <= '9') {
                        try {
                            long long1 = Long.parseLong(part1);
                            long long2 = Long.parseLong(part2);
                            if (long1 == long2) {
                                continue;
                            }
                            return long1 > long2 ? 1 : -1;
                        } catch (NumberFormatException e) {
                            // fallthrough to string compare
                        }
                    }
                }
                int compareString = part1.compareTo(part2);
                if (compareString != 0) {
                    return compareString;
                }
            }
            return 0;
        }
    }
}
