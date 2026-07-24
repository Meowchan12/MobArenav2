package vn.meowchan12.mobarenav2.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import vn.meowchan12.mobarenav2.MobArenav2;
import vn.meowchan12.mobarenav2.arena.Arena;

import java.util.List;

public class ArenaProtectionListener implements Listener {

    private final MobArenav2 plugin;

    public ArenaProtectionListener(MobArenav2 plugin) {
        this.plugin = plugin;
    }

    // --- ANTI-DUPE: LOCK INVENTORY DURING 3-SECOND COUNTDOWN ---
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (plugin.getInventoryBackupManager() != null && plugin.getInventoryBackupManager().isPendingJoin(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage("§c[MobArenav2] You cannot move items while preparing to join the arena!");
            }
        }
    }

    // --- ANTI-DUPE: LOCK BLOCK INTERACTIONS (CHESTS/ENDER CHESTS) ---
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (plugin.getInventoryBackupManager() != null && plugin.getInventoryBackupManager().isPendingJoin(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // --- ITEM DROP PREVENTION ---
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        // 1. Block dropping items during the 3-second countdown
        if (plugin.getInventoryBackupManager() != null && plugin.getInventoryBackupManager().isPendingJoin(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("§c[MobArenav2] You cannot drop items while preparing to join the arena!");
            return;
        }

        // 2. Block dropping items inside the arena based on config
        Arena arena = plugin.getArenaManager().getArenaByPlayer(player);
        if (arena != null && arena.isRunning()) {
            boolean allowDrop = arena.getConfig().getBoolean("settings.rules.item-drop", false);
            if (!allowDrop) {
                event.setCancelled(true);
                player.sendMessage("§c[MobArenav2] You cannot drop items in this arena!");
            }
        }
    }

    // --- BLOCK BREAKING PREVENTION ---
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.getSetupManager().isInSetupMode(event.getPlayer())) return;

        Arena arena = plugin.getArenaManager().getArenaByPlayer(event.getPlayer());
        if (arena != null && arena.isRunning()) {
            boolean allowBreak = arena.getConfig().getBoolean("settings.rules.block-break", false);
            if (!allowBreak) event.setCancelled(true);
        }
    }

    // --- BLOCK PLACING PREVENTION ---
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (plugin.getSetupManager().isInSetupMode(event.getPlayer())) return;

        Arena arena = plugin.getArenaManager().getArenaByPlayer(event.getPlayer());
        if (arena != null && arena.isRunning()) {
            boolean allowPlace = arena.getConfig().getBoolean("settings.rules.block-place", false);
            if (!allowPlace) event.setCancelled(true);
        }
    }

    // --- COMMAND WHITELIST (Applies to Lobby & Spectator) ---
    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (plugin.getSetupManager().isInSetupMode(player)) return;

        Arena arena = plugin.getArenaManager().getArenaByPlayer(player);

        if (arena != null) {
            boolean allowAllCmds = arena.getConfig().getBoolean("settings.rules.allow-commands", false);
            if (allowAllCmds) return;

            String cmd = event.getMessage().toLowerCase();
            List<String> whitelist = arena.getConfig().getStringList("settings.rules.command-whitelist");
            boolean isAllowed = false;

            if (cmd.startsWith("/ma ") || cmd.equals("/ma")) {
                isAllowed = true;
            } else {
                for (String allowedCmd : whitelist) {
                    if (cmd.startsWith(allowedCmd.toLowerCase())) {
                        isAllowed = true;
                        break;
                    }
                }
            }

            if (!isAllowed) {
                event.setCancelled(true);
                player.sendMessage("§c[MobArenav2] This command cannot be used while inside the arena! Please use /ma leave to exit.");
            }
        }
    }

    // --- ANTI-ESCAPE: TELEPORTATION BLOCKER ---
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.getArenaManager().getArenaByPlayer(player);
        if (arena == null) return;

        Location to = event.getTo();
        if (to == null) return;
        PlayerTeleportEvent.TeleportCause cause = event.getCause();

        // 1. Block teleportation by other plugins (e.g., /spawn, /home, /tp)
        if (cause == PlayerTeleportEvent.TeleportCause.COMMAND || cause == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            boolean isValidDest = false;

            // Differentiate internal MobArena teleports (Lobby, Spectator Spawn, Arena Spawn)
            if (isSameLocationSafe(to, arena.getLobby())) isValidDest = true;
            else if (isSameLocationSafe(to, arena.getSpectatorSpawn())) isValidDest = true;
            else if (isSameLocationSafe(to, arena.getArenaSpawn())) isValidDest = true;
            else if (arena.containsLocation(to)) isValidDest = true;

            // If the destination is outside the arena boundaries => Cancel teleport
            // Note: Valid /ma leave commands won't be trapped here as the player is removed from the arena map prior to teleportation.
            if (!isValidDest) {
                event.setCancelled(true);
                player.sendMessage("§c[MobArenav2] Teleportation has been blocked! You must type §e/ma leave §cto safely exit the arena.");
            }
        }

        // 2. Block using Ender Pearls to escape combat boundaries
        if (cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            if (arena.isRunning() && !arena.containsLocation(to)) {
                event.setCancelled(true);
                player.sendMessage("§c[MobArenav2] You cannot use an Ender Pearl to escape the combat boundaries!");
            }
        }
    }

    // Utility method to check safe coordinates (tolerance radius)
    private boolean isSameLocationSafe(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null || loc1.getWorld() != loc2.getWorld()) return false;
        return loc1.distanceSquared(loc2) < 5.0; // Margin of error ~2 blocks
    }

    // --- PREVENT NATURAL MOB SPAWNING ---
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM ||
                event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            return;
        }

        for (Arena arena : plugin.getArenaManager().getAllArenas()) {
            if (arena.containsLocation(event.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // --- SUN IMMUNITY FOR ARENA MOBS ---
    @EventHandler
    public void onEntityCombust(EntityCombustEvent event) {
        if (event instanceof EntityCombustByEntityEvent || event instanceof EntityCombustByBlockEvent) {
            return;
        }

        for (Arena arena : plugin.getArenaManager().getAllArenas()) {
            if (arena.isRunning() && arena.getActiveMobs().contains(event.getEntity())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // --- PREVENT PLAYER PVP BASED ON CONFIG ---
    @EventHandler
    public void onPlayerPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;

        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker != null) {
            Arena arena = plugin.getArenaManager().getArenaByPlayer(victim);
            if (arena != null && plugin.getArenaManager().getArenaByPlayer(attacker) == arena) {

                boolean allowPvP = arena.getConfig().getBoolean("settings.rules.player-pvp", false);
                if (!allowPvP) {
                    event.setCancelled(true);
                }
            }
        }
    }
}