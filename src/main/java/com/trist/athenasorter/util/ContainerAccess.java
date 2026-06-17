package com.trist.athenasorter.util;

import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.World;

public final class ContainerAccess {
    private ContainerAccess() {}

    public static ItemContainer getContainerAt(World world, int x, int y, int z) {
        if (world == null) {
            return null;
        }
        try {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
            if (world.getChunkIfLoaded(chunkIndex) == null) {
                return null;
            }
            if (!ContainerBlockUtil.isLikelyContainer(world.getBlockType(x, y, z))) {
                return null;
            }
            ItemContainerBlock block =
                    (ItemContainerBlock) BlockModule.getComponent(
                            (ComponentType) ItemContainerBlock.getComponentType(), world, x, y, z);
            if (block != null) {
                return block.getItemContainer();
            }
            ProcessingBenchBlock bench =
                    (ProcessingBenchBlock) BlockModule.getComponent(
                            (ComponentType) ProcessingBenchBlock.getComponentType(), world, x, y, z);
            if (bench != null) {
                return bench.getItemContainer();
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
