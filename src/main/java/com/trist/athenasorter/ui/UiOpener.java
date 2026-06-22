package com.trist.athenasorter.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.trist.athenasorter.AthenaSorterMod;
import com.trist.athenasorter.manager.StorageManager;
import org.joml.Vector3i;

public final class UiOpener {
    private UiOpener() {}

    public static void openControlPanel(Ref<EntityStore> entityRef, Store<EntityStore> store, PlayerRef playerRef) {
        openControlPanelTab(entityRef, store, playerRef, 0);
    }

    public static void openControlPanelTab(
            Ref<EntityStore> entityRef, Store<EntityStore> store, PlayerRef playerRef, int legacyTab) {
        AthenaSorterMod mod = AthenaSorterMod.getInstance();
        if (mod == null) {
            return;
        }
        StorageManager manager = mod.getStorageManager();
        if (manager == null) {
            return;
        }
        int tab = legacyTab + ChestConfigPage.TAB_HELP;
        openCustomPage(entityRef, store, new ChestConfigPage(playerRef, manager, tab));
    }

    public static void openChestConfig(
            Ref<EntityStore> entityRef,
            Store<EntityStore> store,
            PlayerRef playerRef,
            String blockId,
            StorageManager manager,
            Vector3i targetBlock,
            String worldName,
            String heldItemId) {
        openCustomPage(
                entityRef,
                store,
                new ChestConfigPage(
                        playerRef, blockId, manager, targetBlock, worldName, heldItemId));
    }

    public static void openCustomPage(
            Ref<EntityStore> entityRef,
            Store<EntityStore> store,
            com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage page) {
        if (entityRef == null || !entityRef.isValid() || store == null || page == null) {
            return;
        }
        com.hypixel.hytale.server.core.universe.world.World world = store.getExternalData().getWorld();
        if (world == null) {
            openCustomPageNow(entityRef, store, page);
            return;
        }
        world.execute(() -> openCustomPageNow(entityRef, store, page));
    }

    private static void openCustomPageNow(
            Ref<EntityStore> entityRef,
            Store<EntityStore> store,
            com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage page) {
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }
        com.hypixel.hytale.server.core.entity.entities.Player player =
                store.getComponent(entityRef, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
        if (player == null) {
            return;
        }
        player.getPageManager().openCustomPage(entityRef, store, page);
    }
}
