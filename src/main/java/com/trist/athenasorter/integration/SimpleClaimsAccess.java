package com.trist.athenasorter.integration;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * SimpleClaims integration (Citybuild plots = claimed chunks). Blocks scanning and using
 * containers on foreign plots — same rules as opening a chest with right-click.
 */
public final class SimpleClaimsAccess {
    private static final String CLAIM_MANAGER_CLASS = "com.buuz135.simpleclaims.claim.ClaimManager";
    private static final String PARTY_INFO_CLASS = "com.buuz135.simpleclaims.claim.party.PartyInfo";
    private static final String PERM_INTERACT = "simpleclaims.party.protection.interact";
    private static final String PERM_INTERACT_CHEST = "simpleclaims.party.protection.interact.chest";
    private static final String PERM_INTERACT_BENCH = "simpleclaims.party.protection.interact.bench";

    private static final boolean AVAILABLE = init();
    private static Method getInstance;
    private static Method isAllowedToInteract;
    private static Method isBlockInteractEnabled;
    private static Method isChestInteractEnabled;
    private static Method isBenchInteractEnabled;

    private SimpleClaimsAccess() {}

    private static boolean init() {
        try {
            Class<?> claimManagerClass = Class.forName(CLAIM_MANAGER_CLASS);
            Class<?> partyInfoClass = Class.forName(PARTY_INFO_CLASS);

            getInstance = claimManagerClass.getMethod("getInstance");
            isAllowedToInteract =
                    claimManagerClass.getMethod(
                            "isAllowedToInteract",
                            UUID.class,
                            String.class,
                            int.class,
                            int.class,
                            Predicate.class,
                            String.class);
            isBlockInteractEnabled = partyInfoClass.getMethod("isBlockInteractEnabled");
            isChestInteractEnabled = partyInfoClass.getMethod("isChestInteractEnabled");
            isBenchInteractEnabled = partyInfoClass.getMethod("isBenchInteractEnabled");
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static boolean canUseContainer(
            PlayerRef playerRef, World world, BlockType blockType, int x, int y, int z) {
        if (playerRef == null) {
            return false;
        }
        return canUseContainer(playerRef.getUuid(), world, blockType, x, y, z);
    }

    public static boolean canUseContainer(
            UUID playerUuid, World world, BlockType blockType, int x, int y, int z) {
        if (!AVAILABLE) {
            return true;
        }
        if (playerUuid == null || world == null) {
            return false;
        }
        try {
            String blockId =
                    blockType != null && blockType.getId() != null
                            ? blockType.getId().toLowerCase(Locale.ROOT)
                            : "";
            Method partyMethod = isBlockInteractEnabled;
            String permission = PERM_INTERACT;

            if (isChestLikeContainer(blockId)) {
                partyMethod = isChestInteractEnabled;
                permission = PERM_INTERACT_CHEST;
            } else if (isBenchLikeContainer(blockId)) {
                partyMethod = isBenchInteractEnabled;
                permission = PERM_INTERACT_BENCH;
            }

            Object claimManager = getInstance.invoke(null);
            final Method interactCheck = partyMethod;
            Predicate<Object> predicate =
                    party -> {
                        try {
                            return (boolean) interactCheck.invoke(party);
                        } catch (ReflectiveOperationException e) {
                            return false;
                        }
                    };

            Object allowed =
                    isAllowedToInteract.invoke(
                            claimManager, playerUuid, world.getName(), x, z, predicate, permission);
            return allowed instanceof Boolean && (Boolean) allowed;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isChestLikeContainer(String blockId) {
        return blockId.contains("chest")
                || blockId.contains("barrel")
                || blockId.contains("crate")
                || blockId.contains("stash")
                || blockId.contains("locker")
                || blockId.contains("treasure")
                || blockId.contains("storage");
    }

    private static boolean isBenchLikeContainer(String blockId) {
        return (blockId.contains("bench") && !blockId.contains("furniture"))
                || blockId.contains("furnace")
                || blockId.contains("workbench")
                || blockId.contains("crusher")
                || blockId.contains("alloyer")
                || blockId.contains("armory")
                || blockId.contains("shelf");
    }
}
