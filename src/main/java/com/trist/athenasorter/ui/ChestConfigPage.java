package com.trist.athenasorter.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.trist.athenasorter.manager.StorageManager;
import com.trist.athenasorter.ui.UiOpener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.joml.Vector3i;

public class ChestConfigPage extends InteractiveCustomUIPage<ChestConfigPage.PageData> {
    private static final int ITEMS_PER_PAGE = 12;

    private final String blockId;
    private final StorageManager manager;
    private final Vector3i targetBlock;
    private final String worldName;
    private final String heldItemId;
    private int filterPage = 0;

    public ChestConfigPage(
            PlayerRef playerRef,
            String blockId,
            StorageManager manager,
            Vector3i targetBlock,
            String worldName,
            String heldItemId) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.blockId = blockId;
        this.manager = manager;
        this.targetBlock = targetBlock;
        this.worldName = worldName;
        this.heldItemId = heldItemId;
    }

    @Override
    public void build(
            Ref<EntityStore> ref,
            UICommandBuilder commands,
            UIEventBuilder events,
            Store<EntityStore> store) {
        commands.append("AthenaSorter/ChestConfig.ui");

        StorageManager.ChestRole role =
                manager.getChestRole(worldName, targetBlock.x, targetBlock.y, targetBlock.z);
        String roleText;
        String roleColor;
        switch (role) {
            case INPUT:
                roleText = "Status: EINGANGSKISTE (Items hier reinlegen)";
                roleColor = "#63b3ed";
                break;
            case STORAGE:
                roleText = "Status: SPEICHERKISTE (nimmt zugewiesene Items)";
                roleColor = "#68d391";
                break;
            default:
                roleText = "Status: Noch nicht eingerichtet";
                roleColor = "#f6ad55";
                break;
        }
        commands.set("#RoleBadge.Text", roleText);
        commands.set("#RoleBadge.Style.TextColor", roleColor);

        boolean isStorage = role == StorageManager.ChestRole.STORAGE;
        commands.set("#FilterHint.Visible", isStorage);
        commands.set("#FilterGrid.Visible", isStorage);
        commands.set("#PageControls.Visible", isStorage);
        commands.set("#ClearFiltersBtn.Visible", isStorage);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#SetInputBtn", EventData.of("Action", "set_input"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SetStorageBtn", EventData.of("Action", "set_storage"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ClearRoleBtn", EventData.of("Action", "clear_role"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#RadiusDec", EventData.of("Action", "radius_dec"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#RadiusInc", EventData.of("Action", "radius_inc"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IntervalDec", EventData.of("Action", "interval_dec"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IntervalInc", EventData.of("Action", "interval_inc"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#OpenMenuBtn", EventData.of("Action", "open_menu"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ClearFiltersBtn", EventData.of("Action", "clear_filters"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#PrevPage", EventData.of("Action", "page_prev"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NextPage", EventData.of("Action", "page_next"));

        if (isStorage) {
            for (int i = 0; i < ITEMS_PER_PAGE; i++) {
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#FilterBtn" + i,
                        EventData.of("Action", "filter_click_" + i));
            }
            buildFilterGrid(commands);
        }

        int radius = manager.getSettings().scanRadius;
        float interval = manager.getSettings().scanIntervalSeconds;
        commands.set("#RadiusLabel.Text", "Suchradius: " + radius + " Blöcke");
        commands.set("#IntervalLabel.Text", "Sortier-Intervall: " + String.format("%.1fs", interval));
    }

    private void buildFilterGrid(UICommandBuilder commands) {
        Set<String> items =
                manager.getStorageData(worldName, targetBlock.x, targetBlock.y, targetBlock.z) != null
                        ? manager
                                .getStorageData(worldName, targetBlock.x, targetBlock.y, targetBlock.z)
                                .acceptedItems
                        : Collections.emptySet();
        List<String> sorted = new ArrayList<>(items);
        Collections.sort(sorted);
        int totalPages = Math.max(1, (int) Math.ceil((double) sorted.size() / ITEMS_PER_PAGE));
        if (filterPage >= totalPages) {
            filterPage = totalPages - 1;
        }
        commands.set("#PageLabel.Text", "Seite " + (filterPage + 1) + "/" + totalPages);
        int start = filterPage * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int index = start + i;
            commands.set(
                    "#FilterIcon" + i + ".ItemId",
                    index < sorted.size() ? sorted.get(index) : "");
        }
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, PageData data) {
        if (data == null || data.getAction() == null) {
            rebuild();
            return;
        }
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        String action = data.getAction();
        int x = targetBlock.x;
        int y = targetBlock.y;
        int z = targetBlock.z;
        String uuid = playerRef.getUuid().toString();
        String name = playerRef.getUsername();

        switch (action) {
            case "set_input":
                manager.setInputChest(worldName, x, y, z, uuid, name);
                manager.getContainerRegistry().addContainer(worldName, x, y, z);
                playerRef.sendMessage(Message.raw("Diese Kiste ist jetzt die globale Eingangskiste."));
                break;
            case "set_storage":
                manager.setStorageChest(worldName, x, y, z, uuid, name);
                manager.getContainerRegistry().addContainer(worldName, x, y, z);
                if (heldItemId != null
                        && !heldItemId.isEmpty()
                        && !heldItemId.equals("air")) {
                    manager.addAcceptedItem(worldName, x, y, z, heldItemId);
                    playerRef.sendMessage(Message.raw("Speicherkiste eingerichtet. Item-Typ hinzugefügt."));
                } else {
                    playerRef.sendMessage(
                            Message.raw("Speicherkiste eingerichtet. Item in Hand halten + Slot klicken."));
                }
                break;
            case "clear_role":
                manager.clearChestRole(worldName, x, y, z);
                playerRef.sendMessage(Message.raw("Kiste wurde aus dem System entfernt."));
                break;
            case "clear_filters":
                manager.clearAcceptedItems(worldName, x, y, z);
                playerRef.sendMessage(Message.raw("Alle Item-Filter gelöscht."));
                break;
            case "radius_dec":
                manager.setScanRadius(manager.getSettings().scanRadius - 4);
                break;
            case "radius_inc":
                manager.setScanRadius(manager.getSettings().scanRadius + 4);
                break;
            case "interval_dec":
                manager.setScanInterval(manager.getSettings().scanIntervalSeconds - 0.5f);
                break;
            case "interval_inc":
                manager.setScanInterval(manager.getSettings().scanIntervalSeconds + 0.5f);
                break;
            case "page_prev":
                if (filterPage > 0) {
                    filterPage--;
                }
                break;
            case "page_next":
                filterPage++;
                break;
            case "open_menu":
                UiOpener.openControlPanel(ref, store, playerRef);
                return;
            case "close":
                close();
                return;
            default:
                if (action.startsWith("filter_click_")) {
                    handleFilterClick(action, playerRef);
                }
                break;
        }
        rebuild();
    }

    private void handleFilterClick(String action, PlayerRef playerRef) {
        if (manager.getChestRole(worldName, targetBlock.x, targetBlock.y, targetBlock.z)
                != StorageManager.ChestRole.STORAGE) {
            return;
        }
        Set<String> items =
                manager.getStorageData(worldName, targetBlock.x, targetBlock.y, targetBlock.z).acceptedItems;
        List<String> sorted = new ArrayList<>(items);
        Collections.sort(sorted);
        try {
            int slot = Integer.parseInt(action.substring("filter_click_".length()));
            int index = filterPage * ITEMS_PER_PAGE + slot;
            if (index < sorted.size()) {
                manager.removeAcceptedItem(worldName, targetBlock.x, targetBlock.y, targetBlock.z, sorted.get(index));
            }
        } catch (NumberFormatException ignored) {
            return;
        }
        if (heldItemId != null
                && !heldItemId.isEmpty()
                && !heldItemId.equals("air")
                && !items.contains(heldItemId)) {
            manager.addAcceptedItem(worldName, targetBlock.x, targetBlock.y, targetBlock.z, heldItemId);
            playerRef.sendMessage(Message.raw("Item-Typ zur Speicherkiste hinzugefügt."));
        }
    }

    public static class PageData {
        private String action;

        public static final BuilderCodec<PageData> CODEC =
                BuilderCodec.builder(PageData.class, PageData::new)
                        .append(new KeyedCodec("Action", Codec.STRING), PageData::setAction, PageData::getAction)
                        .add()
                        .build();

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }
}
