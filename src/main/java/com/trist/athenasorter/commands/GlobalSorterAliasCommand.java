package com.trist.athenasorter.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.trist.athenasorter.ui.UiOpener;

/** Legacy alias for /athenasorter. */
public class GlobalSorterAliasCommand extends AbstractPlayerCommand {

  public GlobalSorterAliasCommand() {
    super("globalsorter", "AthenaSorter — Alias für /athenasorter");
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
    world.execute(() -> UiOpener.openControlPanel(ref, store, playerRef));
  }
}
