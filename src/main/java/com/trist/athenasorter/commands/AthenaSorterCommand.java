package com.trist.athenasorter.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.trist.athenasorter.ui.UiOpener;

public class AthenaSorterCommand extends AbstractPlayerCommand {

  public AthenaSorterCommand() {
    super("athenasorter", "AthenaSorter — öffnet das Hauptmenü");
    setAllowsExtraArguments(true);
  }

  @Override
  protected boolean canGeneratePermission() {
    return false;
  }

  @Override
  protected void execute(
      CommandContext context,
      Store<EntityStore> store,
      Ref<EntityStore> ref,
      PlayerRef playerRef,
      World world) {
    int tab = parseTab(context.getInputString());
    world.execute(() -> UiOpener.openControlPanelTab(ref, store, playerRef, tab));
  }

  private int parseTab(String input) {
    String[] tokens = input.toLowerCase().split("\\s+");
    for (int i = 0; i < tokens.length; i++) {
      if (tokens[i].endsWith("athenasorter")
          || tokens[i].endsWith("globalsorter")
          || tokens[i].endsWith("lager")
          || tokens[i].endsWith("gs")) {
        if (i + 1 < tokens.length) {
          return tabFromKeyword(tokens[i + 1]);
        }
        return 0;
      }
    }
    for (String token : tokens) {
      int tab = tabFromKeyword(token);
      if (tab >= 0) {
        return tab;
      }
    }
    return 0;
  }

  private int tabFromKeyword(String keyword) {
    switch (keyword) {
      case "help":
      case "hilfe":
        return 0;
      case "status":
        return 1;
      case "scan":
      case "index":
        return 2;
      case "settings":
      case "einstellungen":
        return 3;
      default:
        return -1;
    }
  }
}
