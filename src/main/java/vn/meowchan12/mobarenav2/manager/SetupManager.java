package vn.meowchan12.mobarenav2.manager;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.meowchan12.mobarenav2.arena.Arena;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SetupManager {

    public record SetupSession(Arena arena, ItemStack[] originalInventory, ItemStack[] originalArmor) {}

    private final Map<UUID, SetupSession> activeSessions = new HashMap<>();

    public void enterSetupMode(Player player, Arena arena) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            player.sendMessage("§cYou are already in setup mode! Type /ma setup to exit.");
            return;
        }

        SetupSession session = new SetupSession(
                arena,
                player.getInventory().getContents().clone(),
                player.getInventory().getArmorContents().clone()
        );
        activeSessions.put(player.getUniqueId(), session);

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        giveSetupTools(player);

        player.sendMessage("§a[MobArenav2] Entered setup mode for arena: §e" + arena.getName());
        player.sendMessage("§7Check your inventory for setup tools.");
    }

    public void exitSetupMode(Player player) {
        SetupSession session = activeSessions.remove(player.getUniqueId());
        if (session != null) {
            player.getInventory().setContents(session.originalInventory());
            player.getInventory().setArmorContents(session.originalArmor());
            player.sendMessage("§a[MobArenav2] Exited setup mode. Your inventory has been restored.");
        }
    }

    public boolean isInSetupMode(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    public Arena getArenaInSetup(Player player) {
        SetupSession session = activeSessions.get(player.getUniqueId());
        return (session != null) ? session.arena() : null;
    }

    private void giveSetupTools(Player player) {
        player.getInventory().setItem(0, createTool(Material.WOODEN_AXE, "§eRegion Wand", "§7Left-Click: §fSet Point 1", "§7Right-Click: §fSet Point 2"));
        player.getInventory().setItem(1, createTool(Material.BONE, "§cMob Spawnpoint Tool", "§7Right-Click: §fAdd Mob Spawnpoint"));
        player.getInventory().setItem(2, createTool(Material.DIAMOND, "§bLobby & Arena Spawn", "§7Left-Click: §fSet Lobby", "§7Right-Click: §fSet Arena Spawn"));

        // Đổi sang COMPASS cho Spectator Spawn để tránh lỗi của Ender Eye
        player.getInventory().setItem(3, createTool(Material.COMPASS, "§5Spectator Spawn", "§7Right-Click/Left-Click: §fSet Spectator Spawn"));

        // Công cụ thiết lập Điểm Thùng Thính
        player.getInventory().setItem(4, createTool(Material.CHEST, "§6Supply Drop Point", "§7Right-Click: §fAdd Supply Drop Location"));

        player.getInventory().setItem(8, createTool(Material.EMERALD, "§aSave & Exit", "§7Right-Click: §fSave configuration and exit setup mode"));
    }

    private ItemStack createTool(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            meta.lore(java.util.Arrays.stream(lore).map(Component::text).toList());
            item.setItemMeta(meta);
        }
        return item;
    }
}