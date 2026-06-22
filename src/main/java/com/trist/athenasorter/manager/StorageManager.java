package com.trist.athenasorter.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.trist.athenasorter.util.ChestRowUtil;

public class StorageManager {
    public enum ChestRole {
        NONE,
        INPUT,
        STORAGE
    }

    public static class StorageChestData {
        public Set<String> acceptedItems = ConcurrentHashMap.newKeySet();
        public String label = "";
        public String ownerUuid = null;
        public String ownerName = "Unbekannt";
        /** When true, each row slot in {@link #rowItems} maps to one chest row. */
        public boolean rowSortMode = false;
        /** Item id per row (index 0 = top row in chest). Length {@link com.trist.athenasorter.util.ChestRowUtil#MAX_ROWS}. */
        public String[] rowItems = new String[com.trist.athenasorter.util.ChestRowUtil.MAX_ROWS];

        public int findRowForItem(String itemId) {
            List<Integer> rows = rowsForItem(itemId);
            return rows.isEmpty() ? -1 : rows.get(0);
        }

        /** All row indices that accept this item (e.g. row 2 and 3 with the same stone). */
        public List<Integer> rowsForItem(String itemId) {
            List<Integer> rows = new ArrayList<>();
            if (!rowSortMode || itemId == null) {
                return rows;
            }
            for (int i = 0; i < rowItems.length; i++) {
                String rowItem = rowItems[i];
                if (rowItem != null && !rowItem.isEmpty() && rowItem.equals(itemId)) {
                    rows.add(i);
                }
            }
            return rows;
        }

        public void syncAcceptedFromRows() {
            acceptedItems.clear();
            if (!rowSortMode) {
                return;
            }
            for (String rowItem : rowItems) {
                if (rowItem != null && !rowItem.isEmpty()) {
                    acceptedItems.add(rowItem);
                }
            }
        }
    }

    public static class WorldData {
        public String inputChestKey = null;
        public Map<String, StorageChestData> storageChests = new ConcurrentHashMap<>();
    }

    public static class GlobalSettings {
        public int scanRadius = 32;
        public float scanIntervalSeconds = 1.0f;
        public String helpHint =
                "Eingangskiste: Items reinlegen. Speicherkiste: Item-Typen zuweisen. Ducken + Rechtsklick auf Kiste.";
    }

    private static final String DATA_DIR = "plugins/AthenaSorter/";
    private static final String LEGACY_DATA_DIR = "plugins/GlobalSorter/";
    private static final String DATA_FILE = DATA_DIR + "data.json";
    private static final String REGISTRY_FILE = DATA_DIR + "registry.json";
    private static final String SETTINGS_FILE = DATA_DIR + "settings.json";

    private File resolveExistingFile(String fileName) {
        File primary = new File(DATA_DIR + fileName);
        if (primary.exists()) {
            return primary;
        }
        File legacy = new File(LEGACY_DATA_DIR + fileName);
        if (legacy.exists()) {
            return legacy;
        }
        return primary;
    }

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, WorldData> worlds = new ConcurrentHashMap<>();
    private final ContainerRegistry containerRegistry = new ContainerRegistry();
    private GlobalSettings settings = new GlobalSettings();

    public String positionKey(String worldName, int x, int y, int z) {
        return normalizeWorld(worldName) + ":" + x + ":" + y + ":" + z;
    }

    public String normalizeWorld(String worldName) {
        return worldName == null ? "default" : worldName.trim().toLowerCase();
    }

    public ContainerRegistry getContainerRegistry() {
        return containerRegistry;
    }

    public GlobalSettings getSettings() {
        return settings;
    }

    public void setScanRadius(int radius) {
        settings.scanRadius = Math.max(4, Math.min(radius, 64));
        saveSettings();
    }

    public void setScanInterval(float seconds) {
        settings.scanIntervalSeconds = Math.max(0.5f, Math.min(seconds, 5.0f));
        saveSettings();
    }

    public ChestRole getChestRole(String worldName, int x, int y, int z) {
        String world = normalizeWorld(worldName);
        WorldData data = worlds.get(world);
        if (data == null) {
            return ChestRole.NONE;
        }
        String key = positionKey(worldName, x, y, z);
        if (data.inputChestKey != null && data.inputChestKey.equals(key)) {
            return ChestRole.INPUT;
        }
        if (data.storageChests.containsKey(key)) {
            return ChestRole.STORAGE;
        }
        return ChestRole.NONE;
    }

    public void setInputChest(String worldName, int x, int y, int z, String ownerUuid, String ownerName) {
        String world = normalizeWorld(worldName);
        WorldData data = worlds.computeIfAbsent(world, w -> new WorldData());
        String key = positionKey(worldName, x, y, z);
        data.inputChestKey = key;
        data.storageChests.remove(key);
        containerRegistry.addContainer(worldName, x, y, z);
        save();
    }

    public void setStorageChest(String worldName, int x, int y, int z, String ownerUuid, String ownerName) {
        String world = normalizeWorld(worldName);
        WorldData data = worlds.computeIfAbsent(world, w -> new WorldData());
        String key = positionKey(worldName, x, y, z);
        if (data.inputChestKey != null && data.inputChestKey.equals(key)) {
            data.inputChestKey = null;
        }
        StorageChestData chest = data.storageChests.computeIfAbsent(key, k -> new StorageChestData());
        chest.ownerUuid = ownerUuid;
        chest.ownerName = ownerName;
        containerRegistry.addContainer(worldName, x, y, z);
        save();
    }

    public void clearChestRole(String worldName, int x, int y, int z) {
        String world = normalizeWorld(worldName);
        WorldData data = worlds.get(world);
        if (data == null) {
            return;
        }
        String key = positionKey(worldName, x, y, z);
        if (data.inputChestKey != null && data.inputChestKey.equals(key)) {
            data.inputChestKey = null;
        }
        data.storageChests.remove(key);
        containerRegistry.removeContainer(worldName, x, y, z);
        save();
    }

    public void clearInputChestOnly(String worldName) {
        String world = normalizeWorld(worldName);
        WorldData data = worlds.get(world);
        if (data == null || data.inputChestKey == null) {
            return;
        }
        data.inputChestKey = null;
        save();
    }

    public StorageChestData getStorageData(String worldName, int x, int y, int z) {
        WorldData data = worlds.get(normalizeWorld(worldName));
        if (data == null) {
            return null;
        }
        StorageChestData chest = data.storageChests.get(positionKey(worldName, x, y, z));
        if (chest != null) {
            ensureRowItemsCapacity(chest);
        }
        return chest;
    }

    public void addAcceptedItem(String worldName, int x, int y, int z, String itemId) {
        StorageChestData chest = getOrCreateStorage(worldName, x, y, z);
        chest.acceptedItems.add(itemId);
        save();
    }

    public void removeAcceptedItem(String worldName, int x, int y, int z, String itemId) {
        StorageChestData chest = getStorageData(worldName, x, y, z);
        if (chest != null) {
            chest.acceptedItems.remove(itemId);
            if (chest.acceptedItems.isEmpty()) {
                clearChestRole(worldName, x, y, z);
            } else {
                save();
            }
        }
    }

    public void clearAcceptedItems(String worldName, int x, int y, int z) {
        StorageChestData chest = getStorageData(worldName, x, y, z);
        if (chest != null) {
            chest.acceptedItems.clear();
            chest.rowSortMode = false;
            for (int i = 0; i < chest.rowItems.length; i++) {
                chest.rowItems[i] = null;
            }
            clearChestRole(worldName, x, y, z);
        }
    }

    public void setRowSortMode(String worldName, int x, int y, int z, boolean enabled) {
        StorageChestData chest = getOrCreateStorage(worldName, x, y, z);
        chest.rowSortMode = enabled;
        if (!enabled) {
            for (int i = 0; i < chest.rowItems.length; i++) {
                chest.rowItems[i] = null;
            }
        } else {
            chest.syncAcceptedFromRows();
        }
        save();
    }

    public void setRowItem(String worldName, int x, int y, int z, int rowIndex, String itemId) {
        if (rowIndex < 0 || rowIndex >= ChestRowUtil.MAX_ROWS) {
            return;
        }
        StorageChestData chest = getOrCreateStorage(worldName, x, y, z);
        chest.rowItems[rowIndex] = itemId;
        chest.rowSortMode = true;
        chest.syncAcceptedFromRows();
        save();
    }

    public void clearRowItem(String worldName, int x, int y, int z, int rowIndex) {
        StorageChestData chest = getStorageData(worldName, x, y, z);
        if (chest == null || rowIndex < 0 || rowIndex >= chest.rowItems.length) {
            return;
        }
        chest.rowItems[rowIndex] = null;
        chest.syncAcceptedFromRows();
        if (chest.rowSortMode && chest.acceptedItems.isEmpty()) {
            clearChestRole(worldName, x, y, z);
        } else {
            save();
        }
    }

    private StorageChestData getOrCreateStorage(String worldName, int x, int y, int z) {
        String world = normalizeWorld(worldName);
        WorldData data = worlds.computeIfAbsent(world, w -> new WorldData());
        String key = positionKey(worldName, x, y, z);
        StorageChestData chest = data.storageChests.computeIfAbsent(key, k -> new StorageChestData());
        ensureRowItemsCapacity(chest);
        return chest;
    }

    private static void ensureRowItemsCapacity(StorageChestData chest) {
        if (chest.rowItems == null || chest.rowItems.length < ChestRowUtil.MAX_ROWS) {
            String[] expanded = new String[ChestRowUtil.MAX_ROWS];
            if (chest.rowItems != null) {
                for (int i = 0; i < chest.rowItems.length && i < ChestRowUtil.MAX_ROWS; i++) {
                    expanded[i] = chest.rowItems[i];
                }
            }
            chest.rowItems = expanded;
        }
    }

    public String getInputChestKey(String worldName) {
        WorldData data = worlds.get(normalizeWorld(worldName));
        return data != null ? data.inputChestKey : null;
    }

    public Map<String, WorldData> getWorlds() {
        return worlds;
    }

    public void removeRegistryEntry(String worldName, int x, int y, int z) {
        containerRegistry.removeContainer(worldName, x, y, z);
    }

    public void removeChestFully(String worldName, int x, int y, int z) {
        clearChestRole(worldName, x, y, z);
    }

    public void save() {
        new File(DATA_DIR).mkdirs();
        try (FileWriter writer = new FileWriter(DATA_FILE)) {
            gson.toJson(worlds, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (FileWriter writer = new FileWriter(REGISTRY_FILE)) {
            gson.toJson(containerRegistry.getRawData(), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        saveSettings();
    }

    public void saveSettings() {
        new File(DATA_DIR).mkdirs();
        try (FileWriter writer = new FileWriter(SETTINGS_FILE)) {
            gson.toJson(settings, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        File dataFile = resolveExistingFile("data.json");
        if (dataFile.exists()) {
            try (FileReader reader = new FileReader(dataFile)) {
                Map<String, WorldData> loaded =
                        gson.fromJson(reader, new TypeToken<Map<String, WorldData>>() {}.getType());
                if (loaded != null) {
                    worlds.putAll(loaded);
                    for (WorldData worldData : worlds.values()) {
                        for (StorageChestData chest : worldData.storageChests.values()) {
                            ensureRowItemsCapacity(chest);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File settingsFile = resolveExistingFile("settings.json");
        if (settingsFile.exists()) {
            try (FileReader reader = new FileReader(settingsFile)) {
                GlobalSettings loaded = gson.fromJson(reader, GlobalSettings.class);
                if (loaded != null) {
                    settings = loaded;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File registryFile = resolveExistingFile("registry.json");
        if (registryFile.exists()) {
            try (FileReader reader = new FileReader(registryFile)) {
                Map<String, Map<Long, Set<Long>>> loaded =
                        gson.fromJson(reader, new TypeToken<Map<String, Map<Long, Set<Long>>>>() {}.getType());
                if (loaded != null) {
                    containerRegistry.loadFromData(loaded);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println(
                "[AthenaSorter] Geladen: "
                        + worlds.size()
                        + " Welten, Radius "
                        + settings.scanRadius
                        + ", Intervall "
                        + settings.scanIntervalSeconds
                        + "s");
    }
}
