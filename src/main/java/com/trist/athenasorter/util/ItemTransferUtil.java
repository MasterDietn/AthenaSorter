package com.trist.athenasorter.util;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.trist.athenasorter.util.ChestRowUtil;
import org.bson.BsonDocument;
import java.util.ArrayList;
import java.util.List;

public final class ItemTransferUtil {
    private ItemTransferUtil() {}

    public static List<SimpleItemContainer> collectSimpleContainers(ItemContainer container) {
        List<SimpleItemContainer> result = new ArrayList<>();
        collectRecursive(container, result);
        return result;
    }

    private static void collectRecursive(ItemContainer container, List<SimpleItemContainer> output) {
        if (container instanceof SimpleItemContainer simple) {
            output.add(simple);
        } else if (container instanceof CombinedItemContainer combined) {
            int count = combined.getContainersSize();
            for (int i = 0; i < count; i++) {
                collectRecursive(combined.getContainer(i), output);
            }
        }
    }

    public static int moveStack(SimpleItemContainer source, short sourceSlot, SimpleItemContainer destination) {
        ItemStack stack = source.getItemStack(sourceSlot);
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        int moved = addToContainer(destination, stack);
        if (moved <= 0) {
            return 0;
        }
        if (moved >= stack.getQuantity()) {
            source.removeItemStackFromSlot(sourceSlot);
        } else {
            source.setItemStackForSlot(sourceSlot, stack.withQuantity(stack.getQuantity() - moved));
        }
        return moved;
    }

    public static int moveStackToRow(
            SimpleItemContainer source,
            short sourceSlot,
            SimpleItemContainer destination,
            int rowIndex,
            int slotsPerRow) {
        ItemStack stack = source.getItemStack(sourceSlot);
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        int moved = addToRow(destination, stack, rowIndex, slotsPerRow);
        if (moved <= 0) {
            return 0;
        }
        if (moved >= stack.getQuantity()) {
            source.removeItemStackFromSlot(sourceSlot);
        } else {
            source.setItemStackForSlot(sourceSlot, stack.withQuantity(stack.getQuantity() - moved));
        }
        return moved;
    }

    public static int addToRow(SimpleItemContainer container, ItemStack stack, int rowIndex, int slotsPerRow) {
        short capacity = container.getCapacity();
        int start = ChestRowUtil.rowStartSlot(rowIndex, slotsPerRow);
        int end = ChestRowUtil.rowEndSlot(rowIndex, slotsPerRow, capacity);
        if (start >= end) {
            return 0;
        }
        return addToSlotRange(container, stack, start, end);
    }

    public static boolean hasSpaceInRow(SimpleItemContainer container, ItemStack stack, int rowIndex, int slotsPerRow) {
        short capacity = container.getCapacity();
        int start = ChestRowUtil.rowStartSlot(rowIndex, slotsPerRow);
        int end = ChestRowUtil.rowEndSlot(rowIndex, slotsPerRow, capacity);
        if (start >= end) {
            return false;
        }
        return hasSpaceInSlotRange(container, stack, start, end);
    }

    private static int addToSlotRange(SimpleItemContainer container, ItemStack stack, int start, int end) {
        String itemId = stack.getItemId();
        int remaining = stack.getQuantity();
        BsonDocument metadata = stack.getMetadata();
        int movedTotal = 0;

        for (int slot = start; slot < end && remaining > 0; slot++) {
            ItemStack existing = container.getItemStack((short) slot);
            if (existing == null || existing.isEmpty() || !existing.getItemId().equals(itemId)) {
                continue;
            }
            BsonDocument existingMeta = existing.getMetadata();
            if (metadata == null && existingMeta != null
                    || metadata != null && !metadata.equals(existingMeta)) {
                continue;
            }
            int maxStack = getMaxStackSize(itemId);
            int space = maxStack - existing.getQuantity();
            if (space <= 0) {
                continue;
            }
            int moved = Math.min(space, remaining);
            ItemStack updated = existing.withQuantity(existing.getQuantity() + moved);
            container.setItemStackForSlot((short) slot, updated);
            ItemStack verify = container.getItemStack((short) slot);
            if (verify != null && verify.getQuantity() == updated.getQuantity()) {
                remaining -= moved;
                movedTotal += moved;
            }
        }

        for (int slot = start; slot < end && remaining > 0; slot++) {
            ItemStack existing = container.getItemStack((short) slot);
            if (existing != null && !existing.isEmpty()) {
                continue;
            }
            int maxStack = getMaxStackSize(itemId);
            int moved = Math.min(maxStack, remaining);
            ItemStack placed = stack.withQuantity(moved);
            container.setItemStackForSlot((short) slot, placed);
            ItemStack verify = container.getItemStack((short) slot);
            if (verify != null && verify.getItemId().equals(itemId) && verify.getQuantity() == moved) {
                remaining -= moved;
                movedTotal += moved;
            }
        }

        return movedTotal;
    }

    private static boolean hasSpaceInSlotRange(SimpleItemContainer container, ItemStack stack, int start, int end) {
        String itemId = stack.getItemId();
        int needed = stack.getQuantity();
        BsonDocument metadata = stack.getMetadata();
        int available = 0;

        for (int slot = start; slot < end; slot++) {
            ItemStack existing = container.getItemStack((short) slot);
            if (existing == null || existing.isEmpty()) {
                available += getMaxStackSize(itemId);
            } else if (existing.getItemId().equals(itemId)) {
                BsonDocument existingMeta = existing.getMetadata();
                if (metadata == null && existingMeta != null
                        || metadata != null && !metadata.equals(existingMeta)) {
                    continue;
                }
                available += getMaxStackSize(itemId) - existing.getQuantity();
            }
            if (available >= needed) {
                return true;
            }
        }
        return available >= needed;
    }

    public static int addToContainer(SimpleItemContainer container, ItemStack stack) {
        String itemId = stack.getItemId();
        int remaining = stack.getQuantity();
        BsonDocument metadata = stack.getMetadata();
        short capacity = container.getCapacity();
        int movedTotal = 0;

        for (short slot = 0; slot < capacity && remaining > 0; slot++) {
            ItemStack existing = container.getItemStack(slot);
            if (existing == null || existing.isEmpty() || !existing.getItemId().equals(itemId)) {
                continue;
            }
            BsonDocument existingMeta = existing.getMetadata();
            if (metadata == null && existingMeta != null
                    || metadata != null && !metadata.equals(existingMeta)) {
                continue;
            }
            int maxStack = getMaxStackSize(itemId);
            int space = maxStack - existing.getQuantity();
            if (space <= 0) {
                continue;
            }
            int moved = Math.min(space, remaining);
            ItemStack updated = existing.withQuantity(existing.getQuantity() + moved);
            container.setItemStackForSlot(slot, updated);
            ItemStack verify = container.getItemStack(slot);
            if (verify != null && verify.getQuantity() == updated.getQuantity()) {
                remaining -= moved;
                movedTotal += moved;
            }
        }

        for (short slot = 0; slot < capacity && remaining > 0; slot++) {
            ItemStack existing = container.getItemStack(slot);
            if (existing != null && !existing.isEmpty()) {
                continue;
            }
            int maxStack = getMaxStackSize(itemId);
            int moved = Math.min(maxStack, remaining);
            ItemStack placed = stack.withQuantity(moved);
            container.setItemStackForSlot(slot, placed);
            ItemStack verify = container.getItemStack(slot);
            if (verify != null && verify.getItemId().equals(itemId) && verify.getQuantity() == moved) {
                remaining -= moved;
                movedTotal += moved;
            }
        }

        return movedTotal;
    }

    public static boolean hasSpaceFor(SimpleItemContainer container, ItemStack stack) {
        String itemId = stack.getItemId();
        int needed = stack.getQuantity();
        BsonDocument metadata = stack.getMetadata();
        short capacity = container.getCapacity();
        int available = 0;

        for (short slot = 0; slot < capacity; slot++) {
            ItemStack existing = container.getItemStack(slot);
            if (existing == null || existing.isEmpty()) {
                available += getMaxStackSize(itemId);
            } else if (existing.getItemId().equals(itemId)) {
                BsonDocument existingMeta = existing.getMetadata();
                if (metadata == null && existingMeta != null
                        || metadata != null && !metadata.equals(existingMeta)) {
                    continue;
                }
                available += getMaxStackSize(itemId) - existing.getQuantity();
            }
            if (available >= needed) {
                return true;
            }
        }
        return available >= needed;
    }

    private static int getMaxStackSize(String itemId) {
        try {
            Object asset = Item.getAssetMap().getAssetMap().get(itemId);
            if (asset instanceof Item item) {
                return item.getMaxStack();
            }
        } catch (Exception ignored) {
            return 64;
        }
        return 64;
    }
}
