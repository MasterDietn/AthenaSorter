package com.trist.athenasorter.util;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

public final class ContainerBlockUtil {
    private ContainerBlockUtil() {}

    public static boolean isContainerBlockId(String blockId) {
        if (blockId == null) {
            return false;
        }
        String id = blockId.toLowerCase();
        if (isAirOrEmptyId(id)) {
            return false;
        }
        return id.contains("chest")
                || id.contains("furnace")
                || id.contains("workbench")
                || id.contains("anvil")
                || id.contains("container")
                || id.contains("barrel")
                || id.contains("box")
                || id.contains("storage")
                || id.contains("crate")
                || id.contains("bench")
                || id.contains("crusher")
                || id.contains("alloyer")
                || id.contains("armory")
                || id.contains("shelf")
                || id.contains("stash")
                || id.contains("locker")
                || id.contains("treasure");
    }

    public static boolean isLikelyContainer(BlockType blockType) {
        return blockType != null && isContainerBlockId(blockType.getId());
    }

    private static boolean isAirOrEmptyId(String id) {
        return id.equals("air")
                || id.equals("empty")
                || id.endsWith(":air")
                || id.endsWith(":empty")
                || id.contains("air_block")
                || id.contains("empty_block");
    }
}
