package com.trist.athenasorter.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.trist.athenasorter.manager.StorageManager;
import com.trist.athenasorter.service.AthenaSorterActions;
import com.trist.athenasorter.util.ChestRowUtil;
import com.trist.athenasorter.util.ContainerAccess;
import com.trist.athenasorter.util.ItemTransferUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.joml.Vector3i;

public class ChestConfigPage extends InteractiveCustomUIPage<ChestConfigPage.PageData> {
    private static final int ITEMS_PER_PAGE = 12;
    public static final int TAB_CHEST = 0;
    public static final int TAB_HELP = 1;
    public static final int TAB_STATUS = 2;
    public static final int TAB_SCAN = 3;
    public static final int TAB_SETTINGS = 4;

    private final PlayerRef playerRef;
    private final String blockId;
    private final StorageManager manager;
    private final Vector3i targetBlock;
    private final String worldName;
    private final String heldItemId;
    private final boolean chestContext;
    private int currentTab;
    private int filterPage = 0;
    private int scanRadius = 24;
    private String scanResultText = "";
    private String feedbackText = "";

    public ChestConfigPage(
            PlayerRef playerRef,
            String blockId,
            StorageManager manager,
            Vector3i targetBlock,
            String worldName,
            String heldItemId) {
        this(playerRef, blockId, manager, targetBlock, worldName, heldItemId, TAB_CHEST);
    }

    /** Opens the unified UI without a chest (e.g. /lager, /athenasorter). */
    public ChestConfigPage(PlayerRef playerRef, StorageManager manager, int initialTab) {
        this(playerRef, "", manager, null, null, null, initialTab);
    }

    private ChestConfigPage(
            PlayerRef playerRef,
            String blockId,
            StorageManager manager,
            Vector3i targetBlock,
            String worldName,
            String heldItemId,
            int initialTab) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.playerRef = playerRef;
        this.blockId = blockId != null ? blockId : "";
        this.manager = manager;
        this.targetBlock = targetBlock;
        this.worldName = worldName;
        this.heldItemId = heldItemId;
        this.chestContext = targetBlock != null && worldName != null;
        this.currentTab = initialTab;
        if (!chestContext && currentTab == TAB_CHEST) {
            this.currentTab = TAB_HELP;
        }
    }

    @Override
    public void build(
            Ref<EntityStore> ref,
            UICommandBuilder commands,
            UIEventBuilder events,
            Store<EntityStore> store) {
        commands.append("AthenaSorterChest.ui");

        bindTabs(events);
        updateTabVisibility(commands);
        updateTabIndicators(commands);

        commands.set("#TabChestBtn.Visible", chestContext);
        commands.set("#FeedbackLabel.Text", feedbackText);
        commands.set("#ScanResult.Text", scanResultText);

        if (chestContext) {
            buildChestView(ref, store, commands, events);
        }

        buildStatusView(commands);
        buildScanView(commands);
        buildSettingsView(commands);
    }

    private void bindTabs(UIEventBuilder events) {
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#TabChestBtn", EventData.of("Action", "tab_chest"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#TabHelpBtn", EventData.of("Action", "tab_help"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#TabStatusBtn", EventData.of("Action", "tab_status"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#TabScanBtn", EventData.of("Action", "tab_scan"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#TabSettingsBtn", EventData.of("Action", "tab_settings"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#RefreshStatusBtn", EventData.of("Action", "refresh_status"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#ClearInputBtn", EventData.of("Action", "clear_input"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#RunScanBtn", EventData.of("Action", "run_scan"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#ScanRadiusDec", EventData.of("Action", "scan_radius_dec"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#ScanRadiusInc", EventData.of("Action", "scan_radius_inc"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#SortRadiusDec", EventData.of("Action", "sort_radius_dec"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#SortRadiusInc", EventData.of("Action", "sort_radius_inc"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#SortIntervalDec", EventData.of("Action", "sort_interval_dec"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#SortIntervalInc", EventData.of("Action", "sort_interval_inc"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"));
    }

    private void updateTabVisibility(UICommandBuilder commands) {
        commands.set("#ChestView.Visible", currentTab == TAB_CHEST && chestContext);
        commands.set("#HelpView.Visible", currentTab == TAB_HELP);
        commands.set("#StatusView.Visible", currentTab == TAB_STATUS);
        commands.set("#ScanView.Visible", currentTab == TAB_SCAN);
        commands.set("#SettingsView.Visible", currentTab == TAB_SETTINGS);
    }

    private void updateTabIndicators(UICommandBuilder commands) {
        commands.set("#TabChestActive.Visible", currentTab == TAB_CHEST && chestContext);
        commands.set("#TabHelpActive.Visible", currentTab == TAB_HELP);
        commands.set("#TabStatusActive.Visible", currentTab == TAB_STATUS);
        commands.set("#TabScanActive.Visible", currentTab == TAB_SCAN);
        commands.set("#TabSettingsActive.Visible", currentTab == TAB_SETTINGS);
    }

    private void buildChestView(
            Ref<EntityStore> ref,
            Store<EntityStore> store,
            UICommandBuilder commands,
            UIEventBuilder events) {
        int x = targetBlock.x;
        int y = targetBlock.y;
        int z = targetBlock.z;

        StorageManager.ChestRole role = manager.getChestRole(worldName, x, y, z);
        String roleText;
        switch (role) {
            case INPUT:
                roleText = "Status: EINGANGSKISTE (Items hier reinlegen)";
                break;
            case STORAGE:
                roleText = "Status: SPEICHERKISTE (nimmt zugewiesene Items)";
                break;
            default:
                roleText = "Status: Noch nicht eingerichtet";
                break;
        }
        commands.set("#RoleBadge.Text", roleText);

        boolean isStorage = role == StorageManager.ChestRole.STORAGE;
        StorageManager.StorageChestData chestData = manager.getStorageData(worldName, x, y, z);
        boolean rowMode = chestData != null && chestData.rowSortMode;

        commands.set("#FilterHint.Visible", isStorage);
        commands.set("#ToggleRowModeBtn.Visible", isStorage);
        commands.set("#FilterGrid.Visible", isStorage && !rowMode);
        commands.set("#FilterGrid.HitTestVisible", isStorage && !rowMode);
        commands.set("#PageControls.Visible", isStorage && !rowMode);
        commands.set("#PageControls.HitTestVisible", isStorage && !rowMode);
        commands.set("#RowGrid.Visible", isStorage && rowMode);
        commands.set("#RowGrid.HitTestVisible", isStorage && rowMode);
        commands.set("#ClearFiltersBtn.Visible", currentTab == TAB_CHEST && isStorage);

        if (isStorage) {
            commands.set(
                    "#FilterHint.Text",
                    rowMode
                            ? "Reihen-Modus: jedes Item nur in seine Reihe"
                            : "Frei-Modus: Item in Hand + Slot klicken");
            commands.set(
                    "#ToggleRowModeLabel.Text",
                    rowMode ? "Reihen-Modus: AN" : "Reihen-Modus: AUS");
        } else {
            commands.set("#ClearFiltersBtn.Visible", false);
        }

        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#SetInputBtn", EventData.of("Action", "set_input"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#SetStorageBtn", EventData.of("Action", "set_storage"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#ClearRoleBtn", EventData.of("Action", "clear_role"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#ClearFiltersBtn", EventData.of("Action", "clear_filters"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#PrevPage", EventData.of("Action", "page_prev"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#NextPage", EventData.of("Action", "page_next"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#ToggleRowModeBtn", EventData.of("Action", "toggle_row_mode"));

        if (isStorage) {
            int chestRows = resolveChestRowCount(ref, store);
            if (rowMode) {
                commands.set(
                        "#RowHint.Text",
                        "Diese Kiste hat "
                                + chestRows
                                + " Reihen. Reihe 1 = oben. Item in Hand + Reihe klicken.");
                for (int i = 0; i < ChestRowUtil.MAX_ROWS; i++) {
                    boolean show = i < chestRows;
                    commands.set("#RowBtn" + i + ".Visible", show);
                    if (show) {
                        events.addEventBinding(
                                CustomUIEventBindingType.Activating,
                                "#RowBtn" + i,
                                EventData.of("Action", "row_click_" + i));
                    }
                }
                buildRowGrid(commands, chestData, chestRows);
            } else {
                for (int i = 0; i < ITEMS_PER_PAGE; i++) {
                    events.addEventBinding(
                            CustomUIEventBindingType.Activating,
                            "#FilterBtn" + i,
                            EventData.of("Action", "filter_click_" + i));
                }
                buildFilterGrid(commands);
            }
        }
    }

    private void buildStatusView(UICommandBuilder commands) {
        Player player = playerRef.getComponent(Player.getComponentType());
        World world = player != null ? player.getWorld() : null;
        AthenaSorterActions.WorldStatus status = AthenaSorterActions.getWorldStatus(manager, world);

        if (status == null) {
            commands.set("#StatusWorld.Text", "Welt: nicht verfügbar");
            commands.set("#StatusInput.Text", "Eingangskiste: —");
            commands.set("#StatusStorage.Text", "Speicherkisten: —");
            commands.set("#StatusIndexed.Text", "Indexierte Kisten: —");
            commands.set("#StatusRadius.Text", "Sortier-Radius: —");
            commands.set("#StatusInterval.Text", "Sortier-Intervall: —");
            return;
        }

        commands.set("#StatusWorld.Text", "Welt: " + status.worldName);
        commands.set(
                "#StatusInput.Text",
                "Eingangskiste: " + AthenaSorterActions.formatInputChest(status.inputChestKey));
        commands.set("#StatusStorage.Text", "Speicherkisten: " + status.storageChestCount);
        commands.set("#StatusIndexed.Text", "Indexierte Kisten: " + status.indexedContainers);
        commands.set("#StatusRadius.Text", "Sortier-Radius: " + status.scanRadius + " Blöcke");
        commands.set(
                "#StatusInterval.Text",
                "Sortier-Intervall: " + String.format("%.1fs", status.scanIntervalSeconds));
    }

    private void buildScanView(UICommandBuilder commands) {
        commands.set("#ScanRadiusLabel.Text", "Scan-Radius: " + scanRadius + " Blöcke");
    }

    private void buildSettingsView(UICommandBuilder commands) {
        int radius = manager.getSettings().scanRadius;
        float interval = manager.getSettings().scanIntervalSeconds;
        commands.set("#SortRadiusLabel.Text", "Sortier-Radius: " + radius + " Blöcke");
        commands.set("#SortIntervalLabel.Text", "Sortier-Intervall: " + String.format("%.1fs", interval));
    }

    private int resolveChestRowCount(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        World world = player != null ? player.getWorld() : null;
        if (world == null || targetBlock == null) {
            return 3;
        }
        ItemContainer container =
                ContainerAccess.getContainerAt(world, targetBlock.x, targetBlock.y, targetBlock.z);
        if (container == null) {
            return 3;
        }
        List<com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer> slots =
                ItemTransferUtil.collectSimpleContainers(container);
        if (slots.isEmpty()) {
            return 3;
        }
        return ChestRowUtil.uiRowCount(slots.get(0).getCapacity());
    }

    private void buildRowGrid(
            UICommandBuilder commands, StorageManager.StorageChestData chestData, int chestRows) {
        for (int i = 0; i < ChestRowUtil.MAX_ROWS; i++) {
            String itemId = "";
            if (i < chestRows
                    && chestData != null
                    && chestData.rowItems != null
                    && i < chestData.rowItems.length) {
                String rowItem = chestData.rowItems[i];
                if (rowItem != null && !rowItem.isEmpty()) {
                    itemId = rowItem;
                }
            }
            commands.set("#RowIcon" + i + ".ItemId", itemId);
        }
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
        PlayerRef activePlayer = store.getComponent(ref, PlayerRef.getComponentType());
        if (activePlayer == null) {
            return;
        }

        String action = data.getAction();
        feedbackText = "";

        switch (action) {
            case "tab_chest":
                if (chestContext) {
                    currentTab = TAB_CHEST;
                }
                break;
            case "tab_help":
                currentTab = TAB_HELP;
                break;
            case "tab_status":
                currentTab = TAB_STATUS;
                break;
            case "tab_scan":
                currentTab = TAB_SCAN;
                break;
            case "tab_settings":
                currentTab = TAB_SETTINGS;
                break;
            case "refresh_status":
                currentTab = TAB_STATUS;
                feedbackText = "Status aktualisiert.";
                break;
            case "clear_input":
                Player player = activePlayer.getComponent(Player.getComponentType());
                AthenaSorterActions.clearInputChest(manager, player != null ? player.getWorld() : null);
                currentTab = TAB_STATUS;
                feedbackText = "Eingangskiste wurde entfernt.";
                activePlayer.sendMessage(Message.raw("Eingangskiste entfernt."));
                break;
            case "scan_radius_dec":
                scanRadius = Math.max(4, scanRadius - 4);
                currentTab = TAB_SCAN;
                break;
            case "scan_radius_inc":
                scanRadius = Math.min(64, scanRadius + 4);
                currentTab = TAB_SCAN;
                break;
            case "run_scan":
                AthenaSorterActions.ScanResult scanResult =
                        AthenaSorterActions.scanNearbyContainers(activePlayer, manager, scanRadius);
                currentTab = TAB_SCAN;
                if (scanResult.failed) {
                    scanResultText = "Scan fehlgeschlagen. Stehe in einer gültigen Welt.";
                    feedbackText = "Scan fehlgeschlagen.";
                } else {
                    scanResultText = "Scan abgeschlossen! " + scanResult.registered + " Kisten registriert.";
                    if (scanResult.skippedForeign > 0 || scanResult.removedForeign > 0) {
                        scanResultText +=
                                " ("
                                        + scanResult.skippedForeign
                                        + " fremde übersprungen, "
                                        + scanResult.removedForeign
                                        + " entfernt)";
                    }
                    feedbackText = scanResult.registered + " eigene Kisten im Index.";
                    activePlayer.sendMessage(
                            Message.raw(
                                    "Index: "
                                            + scanResult.registered
                                            + " Kisten (fremde Plots werden ignoriert)."));
                }
                break;
            case "sort_radius_dec":
                manager.setScanRadius(manager.getSettings().scanRadius - 4);
                currentTab = TAB_SETTINGS;
                feedbackText = "Sortier-Radius geändert.";
                break;
            case "sort_radius_inc":
                manager.setScanRadius(manager.getSettings().scanRadius + 4);
                currentTab = TAB_SETTINGS;
                feedbackText = "Sortier-Radius geändert.";
                break;
            case "sort_interval_dec":
                manager.setScanInterval(manager.getSettings().scanIntervalSeconds - 0.5f);
                currentTab = TAB_SETTINGS;
                feedbackText = "Intervall geändert.";
                break;
            case "sort_interval_inc":
                manager.setScanInterval(manager.getSettings().scanIntervalSeconds + 0.5f);
                currentTab = TAB_SETTINGS;
                feedbackText = "Intervall geändert.";
                break;
            case "close":
                close();
                return;
            default:
                if (chestContext) {
                    handleChestAction(action, activePlayer);
                }
                break;
        }
        rebuild();
    }

    private void handleChestAction(String action, PlayerRef activePlayer) {
        int x = targetBlock.x;
        int y = targetBlock.y;
        int z = targetBlock.z;
        String uuid = activePlayer.getUuid().toString();
        String name = activePlayer.getUsername();

        switch (action) {
            case "set_input":
                manager.setInputChest(worldName, x, y, z, uuid, name);
                manager.getContainerRegistry().addContainer(worldName, x, y, z);
                activePlayer.sendMessage(Message.raw("Diese Kiste ist jetzt die globale Eingangskiste."));
                currentTab = TAB_CHEST;
                break;
            case "set_storage":
                manager.setStorageChest(worldName, x, y, z, uuid, name);
                manager.getContainerRegistry().addContainer(worldName, x, y, z);
                if (heldItemId != null && !heldItemId.isEmpty() && !heldItemId.equals("air")) {
                    manager.addAcceptedItem(worldName, x, y, z, heldItemId);
                    activePlayer.sendMessage(Message.raw("Speicherkiste eingerichtet. Item-Typ hinzugefügt."));
                } else {
                    activePlayer.sendMessage(
                            Message.raw("Speicherkiste eingerichtet. Item in Hand halten + Slot klicken."));
                }
                currentTab = TAB_CHEST;
                break;
            case "clear_role":
                manager.clearChestRole(worldName, x, y, z);
                activePlayer.sendMessage(Message.raw("Kiste wurde aus dem System entfernt."));
                currentTab = TAB_CHEST;
                break;
            case "clear_filters":
                manager.clearAcceptedItems(worldName, x, y, z);
                activePlayer.sendMessage(Message.raw("Alle Zuweisungen gelöscht."));
                currentTab = TAB_CHEST;
                break;
            case "toggle_row_mode":
                StorageManager.StorageChestData chest = manager.getStorageData(worldName, x, y, z);
                boolean enable = chest == null || !chest.rowSortMode;
                manager.setRowSortMode(worldName, x, y, z, enable);
                if (enable) {
                    activePlayer.sendMessage(
                            Message.raw("Reihen-Modus aktiv: Item pro Reihe (Reihe 1 = oben in der Kiste)."));
                } else {
                    activePlayer.sendMessage(
                            Message.raw("Frei-Modus aktiv: Items können in der ganzen Kiste landen."));
                }
                currentTab = TAB_CHEST;
                break;
            case "page_prev":
                if (filterPage > 0) {
                    filterPage--;
                }
                currentTab = TAB_CHEST;
                break;
            case "page_next":
                filterPage++;
                currentTab = TAB_CHEST;
                break;
            default:
                if (action.startsWith("filter_click_")) {
                    handleFilterClick(action, activePlayer);
                    currentTab = TAB_CHEST;
                } else if (action.startsWith("row_click_")) {
                    handleRowClick(action, activePlayer);
                    currentTab = TAB_CHEST;
                }
                break;
        }
    }

    private void handleRowClick(String action, PlayerRef activePlayer) {
        if (manager.getChestRole(worldName, targetBlock.x, targetBlock.y, targetBlock.z)
                != StorageManager.ChestRole.STORAGE) {
            return;
        }
        try {
            int row = Integer.parseInt(action.substring("row_click_".length()));
            StorageManager.StorageChestData chest =
                    manager.getStorageData(worldName, targetBlock.x, targetBlock.y, targetBlock.z);
            if (chest != null
                    && chest.rowItems != null
                    && row < chest.rowItems.length
                    && chest.rowItems[row] != null
                    && !chest.rowItems[row].isEmpty()) {
                manager.clearRowItem(worldName, targetBlock.x, targetBlock.y, targetBlock.z, row);
                activePlayer.sendMessage(Message.raw("Reihe " + (row + 1) + " geleert."));
                return;
            }
            if (heldItemId != null && !heldItemId.isEmpty() && !heldItemId.equals("air")) {
                manager.setRowItem(worldName, targetBlock.x, targetBlock.y, targetBlock.z, row, heldItemId);
                activePlayer.sendMessage(
                        Message.raw("Reihe " + (row + 1) + " nimmt jetzt nur dieses Item-Typ."));
            } else {
                activePlayer.sendMessage(
                        Message.raw("Item in die Hand nehmen und dann die Reihe anklicken."));
            }
        } catch (NumberFormatException ignored) {
            return;
        }
    }

    private void handleFilterClick(String action, PlayerRef activePlayer) {
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
                manager.removeAcceptedItem(
                        worldName, targetBlock.x, targetBlock.y, targetBlock.z, sorted.get(index));
            }
        } catch (NumberFormatException ignored) {
            return;
        }
        if (heldItemId != null
                && !heldItemId.isEmpty()
                && !heldItemId.equals("air")
                && !items.contains(heldItemId)) {
            manager.addAcceptedItem(worldName, targetBlock.x, targetBlock.y, targetBlock.z, heldItemId);
            activePlayer.sendMessage(Message.raw("Item-Typ zur Speicherkiste hinzugefügt."));
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
