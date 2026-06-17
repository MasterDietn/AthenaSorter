package com.trist.athenasorter;

import com.trist.athenasorter.commands.AthenaSorterCommand;
import com.trist.athenasorter.commands.GlobalSorterAliasCommand;
import com.trist.athenasorter.commands.LagerCommand;
import com.trist.athenasorter.manager.StorageManager;
import com.trist.athenasorter.system.ChestInteractSystem;
import com.trist.athenasorter.system.ContainerBreakSystem;
import com.trist.athenasorter.system.ContainerPlaceSystem;
import com.trist.athenasorter.system.SortingSystem;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class AthenaSorterMod extends JavaPlugin {
    private static AthenaSorterMod instance;
    private StorageManager storageManager;

    public AthenaSorterMod(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();
        instance = this;
        System.out.println("[AthenaSorter] Starte AthenaSorter Mod...");

        storageManager = new StorageManager();

        getEntityStoreRegistry().registerSystem(new SortingSystem(storageManager));
        getEntityStoreRegistry().registerSystem(new ChestInteractSystem(storageManager));
        getEntityStoreRegistry().registerSystem(new ContainerPlaceSystem(storageManager));
        getEntityStoreRegistry().registerSystem(new ContainerBreakSystem(storageManager));

        getCommandRegistry().registerCommand(new AthenaSorterCommand());
        getCommandRegistry().registerCommand(new GlobalSorterAliasCommand());
        getCommandRegistry().registerCommand(new LagerCommand());

        storageManager.load();
        System.out.println("[AthenaSorter] Bereit. /athenasorter oder /lager für das Hauptmenü.");
    }

    @Override
    protected void shutdown() {
        if (storageManager != null) {
            storageManager.save();
        }
    }

    public static AthenaSorterMod getInstance() {
        return instance;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }
}
