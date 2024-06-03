package de.diddiz.LogBlock.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Adapted from https://gist.github.com/evilmidget38/26d70114b834f71fb3b4

public class UUIDFetcher {

    private static final String PROFILE_URL = "https://api.mojang.com/profiles/minecraft";
    private static final Gson gson = new GsonBuilder().setLenient().create();

    public static Map<String, UUID> getUUIDs(List<String> names) throws Exception {
        Map<String, UUID> uuidMap = new HashMap<>();
        HttpURLConnection connection = createConnection();
        String body = gson.toJson(names);
        writeBody(connection, body);
        JsonArray array = gson.fromJson(new InputStreamReader(connection.getInputStream()), JsonArray.class);
        for (JsonElement profile : array) {
            JsonObject jsonProfile = (JsonObject) profile;
            String id = jsonProfile.get("id").getAsString();
            String name = jsonProfile.get("name").getAsString();
            UUID uuid = getUUID(id);
            uuidMap.put(name, uuid);
        }
        return uuidMap;
    }

    private static void writeBody(HttpURLConnection connection, String body) throws Exception {
        OutputStream stream = connection.getOutputStream();
        stream.write(body.getBytes());
        stream.flush();
        stream.close();
    }

    private static HttpURLConnection createConnection() throws Exception {
        URL url = new URI(PROFILE_URL).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        return connection;
    }

    private static UUID getUUID(String id) {
        return UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20, 32));
    }
}
