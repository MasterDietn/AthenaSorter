package com.trist.athenasorter.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent.Pre;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.trist.athenasorter.manager.StorageManager;
import com.trist.athenasorter.ui.ChestConfigPage;
import com.trist.athenasorter.util.ContainerBlockUtil;
import org.joml.Vector3i;

/** Opens the config UI on crouch + use on a container block. */
public class ChestInteractSystem extends EntityEventSystem<EntityStore, Pre> {
    private final StorageManager manager;

    public ChestInteractSystem(StorageManager manager) {
        super(Pre.class);
        this.manager = manager;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }

    @Override
    public void handle(
            int index,
            ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer,
            Pre event) {
        BlockType blockType = event.getBlockType();
        if (!ContainerBlockUtil.isLikelyContainer(blockType)) {
            return;
        }
        Vector3i target = event.getTargetBlock();
        if (target == null) {
            return;
        }
        Ref<EntityStore> entityRef =
                event.getContext() != null ? event.getContext().getOwningEntity() : null;
        if (entityRef == null) {
            return;
        }
        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        MovementStatesComponent movement = store.getComponent(entityRef, MovementStatesComponent.getComponentType());
        if (movement == null
                || movement.getMovementStates() == null
                || !movement.getMovementStates().crouching) {
            return;
        }

        String worldName = "default";
        World world = player.getWorld();
        if (world != null) {
            worldName = world.getName();
        }
        manager.getContainerRegistry().addContainer(worldName, target.x, target.y, target.z);

        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        ItemStack held = event.getContext().getHeldItem();
        String heldItemId = held != null && !held.isEmpty() ? held.getItemId() : null;

        player.getPageManager()
                .openCustomPage(
                        entityRef,
                        store,
                        new ChestConfigPage(
                                playerRef,
                                blockType.getId(),
                                manager,
                                target,
                                worldName,
                                heldItemId));
        event.setCancelled(true);
    }
}
