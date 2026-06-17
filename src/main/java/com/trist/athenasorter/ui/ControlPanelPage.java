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
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.trist.athenasorter.manager.StorageManager;
import com.trist.athenasorter.service.AthenaSorterActions;

public class ControlPanelPage extends InteractiveCustomUIPage<ControlPanelPage.PageData> {
    private final PlayerRef playerRef;
    private final StorageManager manager;
    private int currentTab;
    private int scanRadius = 24;
    private String scanResultText = "";
    private String feedbackText = "";

    public ControlPanelPage(PlayerRef playerRef, StorageManager manager, int initialTab) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.playerRef = playerRef;
        this.manager = manager;
        this.currentTab = initialTab;
    }

    @Override
    public void build(
            Ref<EntityStore> ref,
            UICommandBuilder commands,
            UIEventBuilder events,
            Store<EntityStore> store) {
        commands.append("AthenaSorter/ControlPanel.ui");

        bindTabs(events);
        updateTabVisibility(commands);
        highlightTabs(commands);

        buildStatusView(commands);
        buildScanView(commands);
        buildSettingsView(commands);

        commands.set("#ScanResult.Text", scanResultText);
        commands.set("#FeedbackLabel.Text", feedbackText);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"));
    }

    private void bindTabs(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabHelpBtn", EventData.of("Action", "tab_help"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabStatusBtn", EventData.of("Action", "tab_status"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabScanBtn", EventData.of("Action", "tab_scan"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#TabSettingsBtn", EventData.of("Action", "tab_settings"));
        events.addEventBinding(
                CustomUIEventBindingType.Activating, "#RefreshStatusBtn", EventData.of("Action", "refresh_status"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ClearInputBtn", EventData.of("Action", "clear_input"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#RunScanBtn", EventData.of("Action", "run_scan"));
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
    }

    private void updateTabVisibility(UICommandBuilder commands) {
        commands.set("#HelpView.Visible", currentTab == 0);
        commands.set("#StatusView.Visible", currentTab == 1);
        commands.set("#ScanView.Visible", currentTab == 2);
        commands.set("#SettingsView.Visible", currentTab == 3);
    }

    private void highlightTabs(UICommandBuilder commands) {
        String active = "#3498db";
        String inactive = "#2d3748";
        commands.set("#TabHelpBtn.Style.Default.Background.Color", currentTab == 0 ? active : inactive);
        commands.set("#TabStatusBtn.Style.Default.Background.Color", currentTab == 1 ? active : inactive);
        commands.set("#TabScanBtn.Style.Default.Background.Color", currentTab == 2 ? active : inactive);
        commands.set("#TabSettingsBtn.Style.Default.Background.Color", currentTab == 3 ? active : inactive);
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
            commands.set("#StatusRadius.Text", "Suchradius: —");
            commands.set("#StatusInterval.Text", "Intervall: —");
            return;
        }

        commands.set("#StatusWorld.Text", "Welt: " + status.worldName);
        commands.set("#StatusInput.Text", "Eingangskiste: " + AthenaSorterActions.formatInputChest(status.inputChestKey));
        commands.set("#StatusStorage.Text", "Speicherkisten: " + status.storageChestCount);
        commands.set("#StatusIndexed.Text", "Indexierte Kisten: " + status.indexedContainers);
        commands.set("#StatusRadius.Text", "Sortier-Radius: " + status.scanRadius + " Blöcke");
        commands.set("#StatusInterval.Text", "Sortier-Intervall: " + String.format("%.1fs", status.scanIntervalSeconds));
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
        feedbackText = "";

        switch (action) {
            case "tab_help":
                currentTab = 0;
                break;
            case "tab_status":
                currentTab = 1;
                break;
            case "tab_scan":
                currentTab = 2;
                break;
            case "tab_settings":
                currentTab = 3;
                break;
            case "refresh_status":
                currentTab = 1;
                feedbackText = "Status aktualisiert.";
                break;
            case "clear_input":
                Player player = playerRef.getComponent(Player.getComponentType());
                AthenaSorterActions.clearInputChest(manager, player != null ? player.getWorld() : null);
                currentTab = 1;
                feedbackText = "Eingangskiste wurde entfernt.";
                playerRef.sendMessage(Message.raw("Eingangskiste entfernt."));
                break;
            case "scan_radius_dec":
                scanRadius = Math.max(4, scanRadius - 4);
                currentTab = 2;
                break;
            case "scan_radius_inc":
                scanRadius = Math.min(64, scanRadius + 4);
                currentTab = 2;
                break;
            case "run_scan":
                int found = AthenaSorterActions.scanNearbyContainers(playerRef, manager, scanRadius);
                currentTab = 2;
                if (found < 0) {
                    scanResultText = "Scan fehlgeschlagen. Stehe in einer gültigen Welt.";
                    feedbackText = "Scan fehlgeschlagen.";
                } else {
                    scanResultText = "Scan abgeschlossen! " + found + " Container registriert.";
                    feedbackText = found + " Kisten gefunden.";
                    playerRef.sendMessage(Message.raw("Index: " + found + " Container gefunden."));
                }
                break;
            case "sort_radius_dec":
                manager.setScanRadius(manager.getSettings().scanRadius - 4);
                currentTab = 3;
                feedbackText = "Sortier-Radius geändert.";
                break;
            case "sort_radius_inc":
                manager.setScanRadius(manager.getSettings().scanRadius + 4);
                currentTab = 3;
                feedbackText = "Sortier-Radius geändert.";
                break;
            case "sort_interval_dec":
                manager.setScanInterval(manager.getSettings().scanIntervalSeconds - 0.5f);
                currentTab = 3;
                feedbackText = "Intervall geändert.";
                break;
            case "sort_interval_inc":
                manager.setScanInterval(manager.getSettings().scanIntervalSeconds + 0.5f);
                currentTab = 3;
                feedbackText = "Intervall geändert.";
                break;
            case "close":
                close();
                return;
            default:
                break;
        }
        rebuild();
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
