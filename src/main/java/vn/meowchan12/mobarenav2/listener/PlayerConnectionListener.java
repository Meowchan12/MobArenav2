package vn.meowchan12.mobarenav2.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import vn.meowchan12.mobarenav2.MobArenav2;

public class PlayerConnectionListener implements Listener {

    private final MobArenav2 plugin;

    public PlayerConnectionListener(MobArenav2 plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // 1. Exit setup mode securely if the admin leaves abruptly
        if (plugin.getSetupManager() != null && plugin.getSetupManager().isInSetupMode(player)) {
            plugin.getSetupManager().exitSetupMode(player);
        }

        // 2. ANTI-DUPE: Clear pending state if they disconnect during the 3s countdown
        if (plugin.getInventoryBackupManager() != null && plugin.getInventoryBackupManager().isPendingJoin(player.getUniqueId())) {
            plugin.getInventoryBackupManager().removePending(player.getUniqueId());
        }

        // 3. If the player quits while in the arena (Alive or Spectator)
        // The system will force a Leave, clean states, and trigger restoreBackup() safely.
        if (plugin.getArenaManager().isPlaying(player) || plugin.getArenaManager().isSpectating(player)) {
            plugin.getArenaManager().handlePlayerDisconnect(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // CRASH RECOVERY: Check if the player has an emergency backup file
        // If they do, and they are not actively in an arena, restore it immediately.
        if (plugin.getInventoryBackupManager() != null && plugin.getInventoryBackupManager().hasBackup(player.getUniqueId())) {
            if (!plugin.getArenaManager().isPlaying(player)) {

                // Wash away any residual arena items/effects
                player.getInventory().clear();
                plugin.getArenaManager().resetPlayerState(player);

                // Refund Survival Inventory
                plugin.getInventoryBackupManager().restoreBackup(player);

                plugin.getLogger().info("[MobArenav2] Restored inventory for " + player.getName() + " after an unexpected disconnect/crash.");
                player.sendMessage("§a[MobArenav2] Server detected an unsafe disconnect. Your survival inventory has been safely restored!");
            }
        }
    }
}