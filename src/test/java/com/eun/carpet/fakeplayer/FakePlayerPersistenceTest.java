package com.eun.carpet.fakeplayer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FakePlayerPersistenceTest {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @TempDir
    Path directory;

    private Path dataFile;
    private AtomicInteger writes;
    private FakePlayerPersistence.Store store;

    @BeforeEach
    void createStore() {
        dataFile = directory.resolve("fake_players.json");
        writes = new AtomicInteger();
        store = new FakePlayerPersistence.Store(dataFile, path -> {
            writes.incrementAndGet();
            return new FileWriter(path.toFile());
        });
    }

    @Test
    void singlePutAndRemoveStillWriteImmediately() throws Exception {
        assertEquals(0, writes.get());
        assertFalse(Files.exists(dataFile));

        store.put(data(FIRST));
        assertEquals(1, writes.get());
        assertTrue(store.contains(FIRST));
        assertEquals(FIRST.toString(), readFile().get(0).getAsJsonObject().get("uuid").getAsString());

        store.remove(SECOND);
        assertEquals(1, writes.get());

        store.remove(FIRST);
        assertEquals(2, writes.get());
        assertFalse(store.contains(FIRST));
        assertTrue(readFile().isEmpty());
    }

    @Test
    void independentlyCapturedUnchangedActionsDoNotWrite() throws Exception {
        store.put(data(FIRST));
        store.put(data(SECOND));
        writes.set(0);
        String original = Files.readString(dataFile);

        store.updateActions(List.of(data(FIRST), data(SECOND)));
        store.updateActions(List.of(data(FIRST), data(SECOND)));

        assertEquals(0, writes.get());
        assertEquals(original, Files.readString(dataFile));
    }

    @Test
    void allChangedPlayersAreSavedInOneWrite() throws Exception {
        List<FakePlayerData> changed = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            UUID uuid = new UUID(0, i);
            store.put(data(uuid));
            FakePlayerData current = data(uuid);
            current.setSneaking(true);
            current.setForward(1.0F);
            current.setActions(List.of(new FakePlayerData.SavedAction("USE", -1, 4, 2, false)));
            changed.add(current);
        }
        writes.set(0);

        store.updateActions(changed);

        assertEquals(1, writes.get());
        assertEquals(20, readFile().size());
        FakePlayerPersistence.Store reloaded = new FakePlayerPersistence.Store(dataFile);
        for (FakePlayerData current : changed) {
            assertTrue(reloaded.get(current.getUuid()).hasSameActions(current));
        }

        // A shutdown pass immediately after the periodic save has no new work.
        store.updateActions(changed);
        assertEquals(1, writes.get());
    }

    @Test
    void emptyAndUntrackedBatchesDoNotWriteOrCreatePlayers() {
        store.updateActions(List.of());
        store.updateActions(List.of(data(FIRST)));

        assertEquals(0, writes.get());
        assertTrue(store.getAll().isEmpty());
        assertFalse(Files.exists(dataFile));
    }

    @Test
    void actionUpdatesPreserveOriginalSpawnMetadataAndJsonFields() throws Exception {
        FakePlayerData saved = data(FIRST);
        store.put(saved);
        JsonObject original = readFile().get(0).getAsJsonObject();
        FakePlayerData current = GSON.fromJson(GSON.toJson(saved)
                .replace("original_bot", "new_bot")
                .replace("minecraft:overworld", "minecraft:the_nether"), FakePlayerData.class);
        current.setSprinting(true);
        current.setStrafing(-1.0F);
        current.setActions(List.of());
        writes.set(0);

        store.updateActions(List.of(current));

        assertEquals(1, writes.get());
        JsonObject updated = readFile().get(0).getAsJsonObject();
        Set<String> spawnFields = Set.of("uuid", "name", "x", "y", "z", "yaw", "pitch", "gameMode", "dimension");
        for (String field : spawnFields) {
            assertEquals(original.get(field), updated.get(field), field);
        }
        assertEquals(original.keySet(), updated.keySet());
        assertTrue(updated.get("sprinting").getAsBoolean());
        assertEquals(-1.0F, updated.get("strafing").getAsFloat());
        assertTrue(updated.getAsJsonArray("actions").isEmpty());
        assertTrue(Files.readString(dataFile).contains("\n  {"), "Keep pretty-printed JSON");
    }

    @Test
    void legacyJsonLoadsWithoutRewritingAndRoundTrips() throws Exception {
        FakePlayerData original = data(FIRST);
        Files.writeString(dataFile, GSON.toJson(List.of(original)));
        FakePlayerPersistence.Store reloaded = new FakePlayerPersistence.Store(dataFile, path -> {
            writes.incrementAndGet();
            return new FileWriter(path.toFile());
        });

        assertEquals(0, writes.get());
        assertTrue(reloaded.get(FIRST).hasSameActions(original));
        reloaded.updateActions(List.of(data(FIRST)));
        assertEquals(0, writes.get());

        original.setActions(List.of(new FakePlayerData.SavedAction("ATTACK", 5, 20, 3, false)));
        reloaded.updateActions(List.of(original));
        assertEquals(1, writes.get());
        JsonObject action = readFile().get(0).getAsJsonObject().getAsJsonArray("actions")
                .get(0).getAsJsonObject();
        assertEquals(Set.of("type", "limit", "interval", "offset", "continuous"), action.keySet());
        assertEquals(GSON.toJsonTree(original.getActions().get(0)), action);
    }

    @Test
    void unchangedCaptureRetriesAnActionWriteThatFailedToOpen() throws Exception {
        store.put(data(FIRST));
        AtomicInteger attempts = new AtomicInteger();
        FakePlayerPersistence.Store retrying = new FakePlayerPersistence.Store(dataFile, path -> {
            if (attempts.incrementAndGet() == 1) throw new IOException("Simulated open failure");
            return new FileWriter(path.toFile());
        });
        FakePlayerData current = data(FIRST);
        current.setSneaking(true);

        retrying.updateActions(List.of(current));
        assertEquals(1, attempts.get());
        assertFalse(readFile().get(0).getAsJsonObject().get("sneaking").getAsBoolean());

        retrying.updateActions(List.of(current));
        assertEquals(2, attempts.get());
        assertTrue(readFile().get(0).getAsJsonObject().get("sneaking").getAsBoolean());

        retrying.updateActions(List.of(current));
        assertEquals(2, attempts.get(), "A successful retry clears the pending write");
    }

    @Test
    void writerCloseFailureAlsoKeepsTheWritePending() throws Exception {
        store.put(data(FIRST));
        AtomicInteger attempts = new AtomicInteger();
        FakePlayerPersistence.Store retrying = new FakePlayerPersistence.Store(dataFile, path -> {
            if (attempts.incrementAndGet() == 1) {
                return new Writer() {
                    @Override
                    public void write(char[] buffer, int offset, int length) {}

                    @Override
                    public void flush() {}

                    @Override
                    public void close() throws IOException {
                        throw new IOException("Simulated close failure");
                    }
                };
            }
            return new FileWriter(path.toFile());
        });
        FakePlayerData current = data(FIRST);
        current.setSprinting(true);

        retrying.updateActions(List.of(current));
        retrying.updateActions(List.of(current));

        assertEquals(2, attempts.get());
        assertTrue(readFile().get(0).getAsJsonObject().get("sprinting").getAsBoolean());
        retrying.updateActions(List.of(current));
        assertEquals(2, attempts.get());
    }

    private JsonArray readFile() throws Exception {
        return JsonParser.parseString(Files.readString(dataFile)).getAsJsonArray();
    }

    private static FakePlayerData data(UUID uuid) {
        return GSON.fromJson("""
                {
                  "uuid": "%s",
                  "name": "original_bot",
                  "x": 1.25,
                  "y": 64.0,
                  "z": -2.5,
                  "yaw": 45.0,
                  "pitch": 10.0,
                  "gameMode": "survival",
                  "dimension": "minecraft:overworld",
                  "sneaking": false,
                  "sprinting": false,
                  "forward": 0.0,
                  "strafing": 0.0,
                  "actions": [{"type":"ATTACK","limit":-1,"interval":1,"offset":0,"continuous":true}]
                }
                """.formatted(uuid), FakePlayerData.class);
    }
}
