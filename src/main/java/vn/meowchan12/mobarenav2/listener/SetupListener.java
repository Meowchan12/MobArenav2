package vn.meowchan12.mobarenav2.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import vn.meowchan12.mobarenav2.MobArenav2;
import vn.meowchan12.mobarenav2.arena.Arena;
import vn.meowchan12.mobarenav2.arena.ArenaRestorer;
import vn.meowchan12.mobarenav2.manager.SetupManager;

public class SetupListener implements Listener {

    private final MobArenav2 plugin;
    private final SetupManager setupManager;

    public SetupListener(MobArenav2 plugin, SetupManager setupManager) {
        this.plugin = plugin;
        this.setupManager = setupManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!setupManager.isInSetupMode(player)) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        Arena arena = setupManager.getArenaInSetup(player);
        if (arena == null) return;

        Action action = event.getAction();
        Location clickLoc = (event.getClickedBlock() != null) ? event.getClickedBlock().getLocation() : player.getLocation();

        event.setCancelled(true);

        Material type = item.getType();

        switch (type) {
            case WOODEN_AXE -> {
                if (action == Action.LEFT_CLICK_BLOCK) {
                    arena.setPoint1(clickLoc);
                    player.sendMessage("§a[MobArenav2] Point 1 set to: §f" + formatLoc(clickLoc));
                } else if (action == Action.RIGHT_CLICK_BLOCK) {
                    arena.setPoint2(clickLoc);
                    player.sendMessage("§a[MobArenav2] Point 2 set to: §f" + formatLoc(clickLoc));
                }
            }

            case BONE -> {
                if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
                    Location spawnLoc = clickLoc.clone().add(0.5, 1.0, 0.5);
                    arena.getSpawnpoints().add(spawnLoc);
                    player.sendMessage("§a[MobArenav2] Added Mob Spawnpoint! Total: §e" + arena.getSpawnpoints().size());
                }
            }

            case DIAMOND -> {
                // Đã gỡ bỏ phần Sneaking để tách biệt hoàn toàn
                if (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR) {
                    arena.setLobby(player.getLocation());
                    player.sendMessage("§a[MobArenav2] Lobby Spawn set to your current location!");
                } else if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
                    arena.setArenaSpawn(player.getLocation());
                    player.sendMessage("§a[MobArenav2] Arena Spawn set to your current location!");
                }
            }

            case COMPASS -> {
                // Xử lý riêng biệt cho chức năng Spectator Spawn
                if (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
                    arena.setSpectatorSpawn(player.getLocation());
                    player.sendMessage("§a[MobArenav2] Spectator Spawn set to your current location!");
                }
            }

            case CHEST -> {
                // Click phải để lưu tọa độ rơi hòm thính
                if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
                    Location supplyLoc = clickLoc.clone().add(0.5, 1.0, 0.5);
                    arena.getSupplypoints().add(supplyLoc);
                    player.sendMessage("§a[MobArenav2] Added Supply Drop Point! Total: §e" + arena.getSupplypoints().size());
                }
            }

            case EMERALD -> {
                if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
                    plugin.getConfigManager().saveArenaCoords(arena);

                    if (arena.getP1() != null && arena.getP2() != null) {
                        player.sendMessage("§e[MobArenav2] Triggering automatic physical backup...");
                        new ArenaRestorer(plugin).backupArena(arena);
                    } else {
                        player.sendMessage("§c[MobArenav2] Warning: Point 1 or Point 2 is missing. Cannot backup physical blocks!");
                    }

                    setupManager.exitSetupMode(player);
                    player.sendMessage("§a[MobArenav2] Arena §e" + arena.getName() + " §aconfiguration successfully saved!");
                }
            }
        }
    }

    private String formatLoc(Location loc) {
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }
}