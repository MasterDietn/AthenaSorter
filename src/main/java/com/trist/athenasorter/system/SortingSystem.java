package com.trist.athenasorter.system;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.trist.athenasorter.manager.StorageManager;
import com.trist.athenasorter.util.ContainerAccess;
import com.trist.athenasorter.util.ContainerBlockUtil;
import com.trist.athenasorter.util.ItemTransferUtil;
import java.util.List;
import java.util.Map;
import org.joml.Vector3i;

/**
 * Reads items from the global input chest and distributes them to storage chests
 * that accept the item type.
 */
public class SortingSystem extends TickingSystem<EntityStore> {
    private final StorageManager manager;
    private float elapsed = 0.0f;

    public SortingSystem(StorageManager manager) {
        this.manager = manager;
    }

    @Override
    public void tick(float dt, int systemIndex, Store<EntityStore> store) {
        elapsed += dt;
        if (elapsed < manager.getSettings().scanIntervalSeconds) {
            return;
        }
        elapsed = 0.0f;

        try {
            for (World world : Universe.get().getWorlds().values()) {
                if (world.getEntityStore() == null || world.getEntityStore().getStore() != store) {
                    continue;
                }
                processWorld(world);
            }
        } catch (Exception e) {
            System.err.println("[AthenaSorter] Sortierfehler: " + e.getMessage());
        }
    }

    private void processWorld(World world) {
        String worldName = world.getName();
        String inputKey = manager.getInputChestKey(worldName);
        if (inputKey == null) {
            return;
        }

        String[] parts = inputKey.split(":");
        if (parts.length != 4) {
            return;
        }
        int inputX = Integer.parseInt(parts[1]);
        int inputY = Integer.parseInt(parts[2]);
        int inputZ = Integer.parseInt(parts[3]);

        long chunkIndex = ChunkUtil.indexChunkFromBlock(inputX, inputZ);
        if (world.getChunkIfLoaded(chunkIndex) == null) {
            return;
        }

        if (!ContainerBlockUtil.isLikelyContainer(world.getBlockType(inputX, inputY, inputZ))) {
            manager.removeChestFully(worldName, inputX, inputY, inputZ);
            return;
        }

        ItemContainer inputContainer = ContainerAccess.getContainerAt(world, inputX, inputY, inputZ);
        if (inputContainer == null) {
            return;
        }

        List<SimpleItemContainer> inputSlots = ItemTransferUtil.collectSimpleContainers(inputContainer);
        if (inputSlots.isEmpty()) {
            return;
        }

        int radius = manager.getSettings().scanRadius;
        StorageManager.WorldData worldData = manager.getWorlds().get(manager.normalizeWorld(worldName));
        if (worldData == null || worldData.storageChests.isEmpty()) {
            return;
        }

        for (SimpleItemContainer inputSlotContainer : inputSlots) {
            short capacity = inputSlotContainer.getCapacity();
            for (short slot = 0; slot < capacity; slot++) {
                ItemStack stack = inputSlotContainer.getItemStack(slot);
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                String itemId = stack.getItemId();
                Vector3i target = findBestStorage(world, worldName, inputX, inputY, inputZ, itemId, radius, worldData);
                if (target == null) {
                    continue;
                }

                ItemContainer destContainer = ContainerAccess.getContainerAt(world, target.x, target.y, target.z);
                if (destContainer == null) {
                    manager.removeRegistryEntry(worldName, target.x, target.y, target.z);
                    continue;
                }

                List<SimpleItemContainer> destSlots = ItemTransferUtil.collectSimpleContainers(destContainer);
                for (SimpleItemContainer dest : destSlots) {
                    if (ItemTransferUtil.hasSpaceFor(dest, stack)) {
                        ItemTransferUtil.moveStack(inputSlotContainer, slot, dest);
                        break;
                    }
                }
            }
        }
    }

    private Vector3i findBestStorage(
            World world,
            String worldName,
            int inputX,
            int inputY,
            int inputZ,
            String itemId,
            int radius,
            StorageManager.WorldData worldData) {
        Vector3i best = null;
        long bestDist = Long.MAX_VALUE;

        for (Map.Entry<String, StorageManager.StorageChestData> entry : worldData.storageChests.entrySet()) {
            StorageManager.StorageChestData chest = entry.getValue();
            if (!chest.acceptedItems.contains(itemId)) {
                continue;
            }

            String[] parts = entry.getKey().split(":");
            if (parts.length != 4) {
                continue;
            }
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);

            if (x == inputX && y == inputY && z == inputZ) {
                continue;
            }

            long distSq =
                    (long) x - inputX * (x - inputX)
                            + (long) y - inputY * (y - inputY)
                            + (long) z - inputZ * (z - inputZ);
            if (distSq > (long) radius * radius) {
                continue;
            }

            long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
            if (world.getChunkIfLoaded(chunkIndex) == null) {
                continue;
            }

            if (!ContainerBlockUtil.isLikelyContainer(world.getBlockType(x, y, z))) {
                manager.removeChestFully(worldName, x, y, z);
                continue;
            }

            if (distSq < bestDist) {
                bestDist = distSq;
                best = new Vector3i(x, y, z);
            }
        }
        return best;
    }
}
