package com.trist.athenasorter.util;

/** Chest layout helpers for row-based sorting. */
public final class ChestRowUtil {
    /** Max assignable rows in UI and persisted data (supports large mod chests). */
    public static final int MAX_ROWS = 8;
    public static final int DEFAULT_SLOTS_PER_ROW = 9;

    private ChestRowUtil() {}

    public static int slotsPerRow(short capacity) {
        if (capacity <= 0) {
            return DEFAULT_SLOTS_PER_ROW;
        }
        if (capacity <= DEFAULT_SLOTS_PER_ROW) {
            return capacity;
        }
        return DEFAULT_SLOTS_PER_ROW;
    }

    public static int rowCount(short capacity) {
        if (capacity <= 0) {
            return 1;
        }
        int perRow = slotsPerRow(capacity);
        return (capacity + perRow - 1) / perRow;
    }

    /** Rows shown in config UI for this chest capacity. */
    public static int uiRowCount(short capacity) {
        return Math.min(rowCount(capacity), MAX_ROWS);
    }

    public static int rowStartSlot(int rowIndex, int slotsPerRow) {
        return rowIndex * slotsPerRow;
    }

    public static int rowEndSlot(int rowIndex, int slotsPerRow, short capacity) {
        return Math.min(rowStartSlot(rowIndex, slotsPerRow) + slotsPerRow, capacity);
    }
}
