package com.trist.athenasorter.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.joml.Vector3i;

/** Spatial index of container block positions per world. */
public class ContainerRegistry {
    private final Map<String, Map<Long, Set<Long>>> worldRegistry = new ConcurrentHashMap<>();

    private long encodePosition(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) y & 0xFFFL) << 26 | (long) z & 0x3FFFFFFL;
    }

    private Vector3i decodePosition(long encoded) {
        int x = (int) (encoded >> 38);
        int y = (int) (encoded >> 26 & 0xFFFL);
        int z = (int) (encoded & 0x3FFFFFFL);
        if (x >= 0x2000000) {
            x -= 0x4000000;
        }
        if (z >= 0x2000000) {
            z -= 0x4000000;
        }
        return new Vector3i(x, y, z);
    }

    private long chunkKeyFromBlock(int x, int z) {
        return (long) (x >> 4) << 32 | (long) (z >> 4) & 0xFFFFFFFFL;
    }

    private long chunkKeyFromChunk(int chunkX, int chunkZ) {
        return (long) chunkX << 32 | (long) chunkZ & 0xFFFFFFFFL;
    }

    private String cleanWorldName(String worldName) {
        return worldName == null ? "default" : worldName.trim().toLowerCase();
    }

    public void addContainer(String worldName, int x, int y, int z) {
        String world = cleanWorldName(worldName);
        worldRegistry
                .computeIfAbsent(world, w -> new ConcurrentHashMap<>())
                .computeIfAbsent(chunkKeyFromBlock(x, z), c -> ConcurrentHashMap.newKeySet())
                .add(encodePosition(x, y, z));
    }

    public void removeContainer(String worldName, int x, int y, int z) {
        String world = cleanWorldName(worldName);
        Map<Long, Set<Long>> chunks = worldRegistry.get(world);
        if (chunks == null) {
            return;
        }
        Set<Long> positions = chunks.get(chunkKeyFromBlock(x, z));
        if (positions != null) {
            positions.remove(encodePosition(x, y, z));
        }
    }

    public List<Vector3i> getNearbyContainers(String worldName, int x, int y, int z, int radius) {
        String world = cleanWorldName(worldName);
        Map<Long, Set<Long>> chunks = worldRegistry.get(world);
        if (chunks == null) {
            return Collections.emptyList();
        }
        List<Vector3i> result = new ArrayList<>();
        int chunkRadius = (radius >> 4) + 1;
        int baseChunkX = x >> 4;
        int baseChunkZ = z >> 4;
        long radiusSq = (long) radius * radius;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                Set<Long> positions = chunks.get(chunkKeyFromChunk(baseChunkX + dx, baseChunkZ + dz));
                if (positions == null) {
                    continue;
                }
                for (long encoded : positions) {
                    Vector3i pos = decodePosition(encoded);
                    long distSq =
                            (long) pos.x - x * (pos.x - x)
                                    + (long) pos.y - y * (pos.y - y)
                                    + (long) pos.z - z * (pos.z - z);
                    if (distSq <= radiusSq) {
                        result.add(pos);
                    }
                }
            }
        }
        return result;
    }

    public List<Vector3i> getAllPositions(String worldName) {
        String world = cleanWorldName(worldName);
        Map<Long, Set<Long>> chunks = worldRegistry.get(world);
        if (chunks == null) {
            return Collections.emptyList();
        }
        List<Vector3i> result = new ArrayList<>();
        for (Set<Long> positions : chunks.values()) {
            for (long encoded : positions) {
                result.add(decodePosition(encoded));
            }
        }
        return result;
    }

    public Map<String, Map<Long, Set<Long>>> getRawData() {
        return worldRegistry;
    }

    public void loadFromData(Map<String, Map<Long, Set<Long>>> data) {
        worldRegistry.clear();
        if (data == null) {
            return;
        }
        for (Map.Entry<String, Map<Long, Set<Long>>> worldEntry : data.entrySet()) {
            String world = cleanWorldName(worldEntry.getKey());
            Map<Long, Set<Long>> sourceChunks = worldEntry.getValue();
            ConcurrentHashMap<Long, Set<Long>> targetChunks = new ConcurrentHashMap<>();
            for (Map.Entry<Long, Set<Long>> chunkEntry : sourceChunks.entrySet()) {
                Long chunkKey = parseLong(chunkEntry.getKey());
                if (chunkKey == null) {
                    continue;
                }
                Set<Long> sourcePositions = chunkEntry.getValue();
                if (sourcePositions == null) {
                    continue;
                }
                Set<Long> targetPositions = ConcurrentHashMap.newKeySet();
                if (sourcePositions instanceof Collection<?> collection) {
                    for (Object value : collection) {
                        Long pos = parseLong(value);
                        if (pos != null) {
                            targetPositions.add(pos);
                        }
                    }
                }
                if (!targetPositions.isEmpty()) {
                    targetChunks.put(chunkKey, targetPositions);
                }
            }
            if (!targetChunks.isEmpty()) {
                worldRegistry.put(world, targetChunks);
            }
        }
    }

    private Long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
