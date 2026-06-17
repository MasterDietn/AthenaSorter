package com.trist.athenasorter.service;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.trist.athenasorter.integration.SimpleClaimsAccess;
import com.trist.athenasorter.manager.StorageManager;
import com.trist.athenasorter.util.ContainerBlockUtil;
import org.joml.Vector3d;
import org.joml.Vector3i;

public final class AthenaSorterActions {
    private AthenaSorterActions() {}

    public static class WorldStatus {
        public final String worldName;
        public final String inputChestKey;
        public final int storageChestCount;
        public final int indexedContainers;
        public final int scanRadius;
        public final float scanIntervalSeconds;

        public WorldStatus(
                String worldName,
                String inputChestKey,
                int storageChestCount,
                int indexedContainers,
                int scanRadius,
                float scanIntervalSeconds) {
            this.worldName = worldName;
            this.inputChestKey = inputChestKey;
            this.storageChestCount = storageChestCount;
            this.indexedContainers = indexedContainers;
            this.scanRadius = scanRadius;
            this.scanIntervalSeconds = scanIntervalSeconds;
        }
    }

    public static WorldStatus getWorldStatus(StorageManager manager, World world) {
        if (world == null) {
            return null;
        }
        String worldName = world.getName();
        String normalized = manager.normalizeWorld(worldName);
        StorageManager.WorldData data = manager.getWorlds().get(normalized);
        int storageCount = data != null ? data.storageChests.size() : 0;
        int indexed = countIndexedInWorld(manager, normalized);
        return new WorldStatus(
                worldName,
                manager.getInputChestKey(worldName),
                storageCount,
                indexed,
                manager.getSettings().scanRadius,
                manager.getSettings().scanIntervalSeconds);
    }

    private static int countIndexedInWorld(StorageManager manager, String normalizedWorld) {
        return manager.getContainerRegistry().getAllPositions(normalizedWorld).size();
    }

    public static String formatInputChest(String inputKey) {
        if (inputKey == null) {
            return "Nicht gesetzt";
        }
        String[] parts = inputKey.split(":");
        if (parts.length == 4) {
            return "X " + parts[1] + "  Y " + parts[2] + "  Z " + parts[3];
        }
        return inputKey;
    }

    public static int scanNearbyContainers(PlayerRef playerRef, StorageManager manager, int radius) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) {
            return -1;
        }
        Store<EntityStore> store = ref.getStore();
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return -1;
        }
        Player player = playerRef.getComponent(Player.getComponentType());
        World world = player != null ? player.getWorld() : null;
        if (world == null) {
            return -1;
        }

        int clampedRadius = Math.max(4, Math.min(radius, 64));
        Vector3d pos = transform.getPosition();
        Vector3i center = new Vector3i((int) pos.x, (int) pos.y, (int) pos.z);
        String worldName = world.getName();
        int found = 0;

        for (int dy = -clampedRadius; dy <= clampedRadius; dy++) {
            for (int dx = -clampedRadius; dx <= clampedRadius; dx++) {
                for (int dz = -clampedRadius; dz <= clampedRadius; dz++) {
                    int x = center.x + dx;
                    int y = center.y + dy;
                    int z = center.z + dz;
                    long chunk = ChunkUtil.indexChunkFromBlock(x, z);
                    if (world.getChunkIfLoaded(chunk) == null) {
                        continue;
                    }
                    BlockType blockType = world.getBlockType(x, y, z);
                    if (!ContainerBlockUtil.isLikelyContainer(blockType)) {
                        continue;
                    }
                    ItemContainerBlock block =
                            (ItemContainerBlock) BlockModule.getComponent(
                                    (ComponentType) ItemContainerBlock.getComponentType(), world, x, y, z);
                    if (block == null) {
                        continue;
                    }
                    if (!SimpleClaimsAccess.canUseContainer(playerRef, world, blockType, x, y, z)) {
                        continue;
                    }
                    manager.getContainerRegistry().addContainer(worldName, x, y, z);
                    found++;
                }
            }
        }
        manager.save();
        return found;
    }

    public static void clearInputChest(StorageManager manager, World world) {
        if (world == null) {
            return;
        }
        manager.clearInputChestOnly(world.getName());
    }
}
