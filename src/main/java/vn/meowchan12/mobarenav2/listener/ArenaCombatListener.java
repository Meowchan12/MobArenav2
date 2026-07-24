package vn.meowchan12.mobarenav2.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import vn.meowchan12.mobarenav2.MobArenav2;
import vn.meowchan12.mobarenav2.arena.Arena;

public class ArenaCombatListener implements Listener {

    private final MobArenav2 plugin;

    public ArenaCombatListener(MobArenav2 plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Arena arena = plugin.getArenaManager().getArenaByPlayer(player);
        if (arena == null || !arena.isRunning()) return;

        // Bỏ qua nếu là Spectator
        if (arena.getSpectators().contains(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // KÍCH HOẠT FAKE DEATH: Nếu lượng máu sau khi nhận đòn <= 0
        if (player.getHealth() - event.getFinalDamage() <= 0) {
            event.setCancelled(true); // Hủy ngay màn hình đỏ của Minecraft
            plugin.getArenaManager().handlePlayerDeath(player, arena); // Bế đi làm Spectator
        }
    }
}