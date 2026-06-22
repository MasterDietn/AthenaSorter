package com.trist.athenasorter.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.trist.athenasorter.integration.SimpleClaimsAccess;
import com.trist.athenasorter.manager.StorageManager;
import com.trist.athenasorter.util.ContainerBlockUtil;
import org.joml.Vector3i;

public class ContainerPlaceSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
    private final StorageManager manager;

    public ContainerPlaceSystem(StorageManager manager) {
        super(PlaceBlockEvent.class);
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
            PlaceBlockEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || item.isEmpty() || !ContainerBlockUtil.isContainerBlockId(item.getItemId())) {
            return;
        }
        Vector3i target = event.getTargetBlock();
        if (target == null) {
            return;
        }
        World world = findWorld(store);
        if (world == null) {
            return;
        }
        com.hypixel.hytale.component.Ref<EntityStore> entityRef = chunk.getReferenceTo(index);
        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef != null
                && !SimpleClaimsAccess.canUseContainer(
                        playerRef, world, world.getBlockType(target.x, target.y, target.z), target.x, target.y, target.z)) {
            return;
        }
        manager.getContainerRegistry().addContainer(world.getName(), target.x, target.y, target.z);
    }

    private World findWorld(Store<EntityStore> store) {
        for (World world : Universe.get().getWorlds().values()) {
            if (world.getEntityStore() != null && world.getEntityStore().getStore() == store) {
                return world;
            }
        }
        return null;
    }
}
