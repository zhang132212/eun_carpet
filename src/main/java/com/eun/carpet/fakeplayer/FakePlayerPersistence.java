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
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FakePlayerPersistence {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type DATA_TYPE = new TypeToken<List<FakePlayerData>>() {}.getType();

    private static class DefaultStore {
        private static final Store INSTANCE = new Store(FabricLoader.getInstance().getConfigDir()
                .resolve("eun_carpet").resolve("fake_players.json"));
    }

    public static void put(FakePlayerData data) {
        DefaultStore.INSTANCE.put(data);
    }

    public static void remove(UUID uuid) {
        DefaultStore.INSTANCE.remove(uuid);
    }

    /** Updates saved actions synchronously, writing once only if any stored player changed. */
    public static void updateActions(Collection<FakePlayerData> currentData) {
        DefaultStore.INSTANCE.updateActions(currentData);
    }

    public static List<FakePlayerData> getAll() {
        return DefaultStore.INSTANCE.getAll();
    }

    public static FakePlayerData get(UUID uuid) {
        return DefaultStore.INSTANCE.get(uuid);
    }

    public static boolean contains(UUID uuid) {
        return DefaultStore.INSTANCE.contains(uuid);
    }

    @FunctionalInterface
    interface WriterFactory {
        Writer open(Path path) throws IOException;
    }

    // Separate file storage from Fabric initialization so persistence can be tested in isolation.
    static final class Store {
        private final Path dataFile;
        private final WriterFactory writerFactory;
        private final ConcurrentMap<UUID, FakePlayerData> cache = new ConcurrentHashMap<>();
        private boolean dirty;

        Store(Path dataFile) {
            this(dataFile, path -> new FileWriter(path.toFile()));
        }

        Store(Path dataFile, WriterFactory writerFactory) {
            this.dataFile = dataFile;
            this.writerFactory = writerFactory;
            try {
                Files.createDirectories(dataFile.getParent());
            } catch (IOException e) {
                e.printStackTrace();
            }
            loadFromFile();
        }

        private void loadFromFile() {
            if (!Files.exists(dataFile)) {
                cache.clear();
                return;
            }
            try (Reader reader = new FileReader(dataFile.toFile())) {
                List<FakePlayerData> list = GSON.fromJson(reader, DATA_TYPE);
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

        private void saveToFile() {
            try (Writer writer = writerFactory.open(dataFile)) {
                GSON.toJson(new ArrayList<>(cache.values()), writer);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            // Closing the writer can also fail, so clear this only after it closes successfully.
            dirty = false;
        }

        void put(FakePlayerData data) {
            cache.put(data.getUuid(), data);
            dirty = true;
            saveToFile();
        }

        void remove(UUID uuid) {
            if (cache.remove(uuid) != null) {
                dirty = true;
                saveToFile();
            }
        }

        void updateActions(Collection<FakePlayerData> currentData) {
            for (FakePlayerData current : currentData) {
                FakePlayerData stored = cache.get(current.getUuid());
                if (stored != null && !stored.hasSameActions(current)) {
                    stored.updateActionsFrom(current);
                    dirty = true;
                }
            }
            if (dirty) saveToFile();
        }

        List<FakePlayerData> getAll() {
            return new ArrayList<>(cache.values());
        }

        FakePlayerData get(UUID uuid) {
            return cache.get(uuid);
        }

        boolean contains(UUID uuid) {
            return cache.containsKey(uuid);
        }
    }
}
