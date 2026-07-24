package vn.meowchan12.mobarenav2.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import vn.meowchan12.mobarenav2.MobArenav2;
import vn.meowchan12.mobarenav2.manager.UpgradeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ArenaPlayerProfile {

    private final UUID playerUUID;
    private double accumulatedVaultMoney = 0.0;
    private final List<String> pendingCommands = new ArrayList<>();

    // ONLY save location here. Inventory is now handled by InventoryBackupManager safely.
    private Location originalLocation;

    private boolean isReady = false;
    private String selectedClass = null;
    private ArenaScoreboard scoreboard;
    private int kills = 0;

    private final Map<String, Integer> activeUpgrades = new HashMap<>();

    public ArenaPlayerProfile(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public boolean isReady() { return isReady; }
    public void setReady(boolean ready) { this.isReady = ready; }
    public String getSelectedClass() { return selectedClass; }
    public void setSelectedClass(String selectedClass) { this.selectedClass = selectedClass; }
    public ArenaScoreboard getScoreboard() { return scoreboard; }
    public void setScoreboard(ArenaScoreboard scoreboard) { this.scoreboard = scoreboard; }
    public Location getOriginalLocation() { return originalLocation; }
    public int getKills() { return kills; }
    public void addKill() { this.kills++; }

    // --- [MỚI] Hỗ trợ hiển thị danh sách kỹ năng QoL ---
    public Map<String, Integer> getOwnedUpgrades() {
        return activeUpgrades;
    }

    public int getUpgradeLevel(String upgradeId) {
        return activeUpgrades.getOrDefault(upgradeId, 0);
    }

    public void addUpgradeLevel(String upgradeId) {
        activeUpgrades.put(upgradeId, getUpgradeLevel(upgradeId) + 1);
    }

    public void clearUpgrades() {
        activeUpgrades.clear();
    }

    public double getUpgradeValue(String type, MobArenav2 plugin) {
        double total = 0.0;
        for (Map.Entry<String, Integer> entry : activeUpgrades.entrySet()) {
            UpgradeManager.UpgradeData data = plugin.getUpgradeManager().getUpgradeData(entry.getKey());
            if (data != null && data.type.equalsIgnoreCase(type)) {
                total += data.baseValue + (entry.getValue() - 1) * data.increment;
            }
        }
        return total;
    }

    // ==========================================
    // SECURE LOCATION SAVING
    // ==========================================
    public void saveOriginalLocation(Player player) {
        // Only save if it's the FIRST time (When initially joining).
        // Absolutely do not overwrite if they are already in a match/spectating!
        if (this.originalLocation != null) return;
        this.originalLocation = player.getLocation();
    }

    public void cacheReward(String rewardString) {
        if (rewardString == null || rewardString.isEmpty()) return;
        String[] rewards = rewardString.split(",");
        for (String rew : rewards) {
            rew = rew.trim();
            if (rew.toLowerCase().startsWith("vault:")) {
                try {
                    accumulatedVaultMoney += Double.parseDouble(rew.split(":")[1]);
                } catch (NumberFormatException ignored) {}
            } else if (rew.toLowerCase().startsWith("cmd(")) {
                int start = rew.indexOf("(") + 1;
                int end = rew.lastIndexOf(")");
                if (start > 0 && end > start) {
                    pendingCommands.add(rew.substring(start, end));
                }
            }
        }
    }

    public void distributeRewards(MobArenav2 plugin) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) return;
        boolean receivedAnything = false;

        if (accumulatedVaultMoney > 0 && plugin.getEconomy() != null) {
            plugin.getEconomy().depositPlayer(player, accumulatedVaultMoney);
            player.sendMessage("§a[MobArenav2] §7You received §e$" + accumulatedVaultMoney + " §7for your efforts!");
            receivedAnything = true;
        }

        for (String cmd : pendingCommands) {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
            receivedAnything = true;
        }

        if (receivedAnything) {
            player.sendMessage("§a[MobArenav2] §7All accumulated rewards have been added to your account.");
        }

        accumulatedVaultMoney = 0.0;
        pendingCommands.clear();
        clearUpgrades();
    }
}