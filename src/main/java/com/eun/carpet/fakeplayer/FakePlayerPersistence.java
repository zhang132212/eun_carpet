package com.eun.carpet.fakeplayer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FakePlayerPersistence {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("eun_carpet");
    private static final Path DATA_FILE = CONFIG_DIR.resolve("fake_players.json");

    private static final ConcurrentMap<UUID, FakePlayerData> cache = new ConcurrentHashMap<>();

    static {
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        }
        loadFromFile();
    }

    private static void loadFromFile() {
        if (!Files.exists(DATA_FILE)) {
            cache.clear();
            return;
        }
        try (Reader reader = new FileReader(DATA_FILE.toFile())) {
            Type listType = new TypeToken<List<FakePlayerData>>() {}.getType();
            List<FakePlayerData> list = GSON.fromJson(reader, listType);
            cache.clear();
            if (list != null) {
                for (FakePlayerData data : list) {
                    cache.put(data.getUuid(), data);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveToFile() {
        try (Writer writer = new FileWriter(DATA_FILE.toFile())) {
            GSON.toJson(new ArrayList<>(cache.values()), writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void put(FakePlayerData data) {
        cache.put(data.getUuid(), data);
        saveToFile();
    }

    public static void remove(UUID uuid) {
        if (cache.remove(uuid) != null) {
            saveToFile();
        }
    }

    public static List<FakePlayerData> getAll() {
        return new ArrayList<>(cache.values());
    }

    public static FakePlayerData get(UUID uuid) {
        return cache.get(uuid);
    }

    public static boolean contains(UUID uuid) {
        return cache.containsKey(uuid);
    }
}