package com.trist.athenasorter.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.trist.athenasorter.AthenaSorterMod;
import com.trist.athenasorter.manager.StorageManager;

public final class UiOpener {
    private UiOpener() {}

    public static void openControlPanel(Ref<EntityStore> entityRef, Store<EntityStore> store, PlayerRef playerRef) {
        openControlPanelTab(entityRef, store, playerRef, 0);
    }

    public static void openControlPanelTab(
            Ref<EntityStore> entityRef, Store<EntityStore> store, PlayerRef playerRef, int tab) {
        AthenaSorterMod mod = AthenaSorterMod.getInstance();
        if (mod == null) {
            return;
        }
        StorageManager manager = mod.getStorageManager();
        if (manager == null) {
            return;
        }
        openCustomPage(entityRef, store, new ControlPanelPage(playerRef, manager, tab));
    }

    public static void openCustomPage(Ref<EntityStore> entityRef, Store<EntityStore> store, CustomUIPage page) {
        if (entityRef == null || !entityRef.isValid() || store == null || page == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        if (world == null) {
            openCustomPageNow(entityRef, store, page);
            return;
        }
        world.execute(() -> openCustomPageNow(entityRef, store, page));
    }

    private static void openCustomPageNow(
            Ref<EntityStore> entityRef, Store<EntityStore> store, CustomUIPage page) {
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }
        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        player.getPageManager().openCustomPage(entityRef, store, page);
    }
}
