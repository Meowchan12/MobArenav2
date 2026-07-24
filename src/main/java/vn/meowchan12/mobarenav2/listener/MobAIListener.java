package vn.meowchan12.mobarenav2.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location; // Thư viện bắt buộc để dùng Location
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import vn.meowchan12.mobarenav2.MobArenav2;
import vn.meowchan12.mobarenav2.arena.Arena;
import vn.meowchan12.mobarenav2.manager.ArenaManager;

public class MobAIListener implements Listener {

    private final ArenaManager arenaManager;

    public MobAIListener(MobArenav2 plugin, ArenaManager arenaManager) {
        this.arenaManager = arenaManager;

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Arena arena : arenaManager.getAllArenas()) {
                if (!arena.isRunning()) continue;

                // VÁ LỖI ÉP KIỂU: Lấy LivingEntity trước rồi mới kiểm tra xem nó có phải là Mob (có não/mục tiêu) không
                for (LivingEntity entity : arena.getActiveMobs()) {
                    if (entity instanceof Mob mob) {
                        if (mob.getTarget() == null || !mob.getTarget().isValid() || !arena.getActivePlayers().contains(mob.getTarget().getUniqueId())) {

                            // Gọi hàm tìm người chơi tự viết bên dưới
                            Player nearest = getNearestPlayer(arena, mob.getLocation());
                            if (nearest != null) {
                                mob.setTarget(nearest);
                            }
                        }
                    }
                }
            }
        }, 20L, 20L);
    }

    // ========================================================
    // BỔ SUNG: Hàm tự động tìm người chơi gần nhất trong Arena
    // ========================================================
    private Player getNearestPlayer(Arena arena, Location loc) {
        Player nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Player p : arena.getPlayersAsObjects()) {
            if (p.getWorld().equals(loc.getWorld())) {
                double dist = p.getLocation().distanceSquared(loc);
                if (dist < minDistance) {
                    minDistance = dist;
                    nearest = p;
                }
            }
        }
        return nearest;
    }

    @EventHandler
    public void onMobFriendlyFire(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        LivingEntity damager = null;

        if (event.getDamager() instanceof LivingEntity) {
            damager = (LivingEntity) event.getDamager();
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity) {
            damager = (LivingEntity) projectile.getShooter();
        }

        if (damager == null) return;

        if (victim.hasMetadata("MA_MOB") && damager.hasMetadata("MA_MOB")) {
            String victimArena = victim.getMetadata("MA_MOB").get(0).asString();
            String damagerArena = damager.getMetadata("MA_MOB").get(0).asString();

            if (victimArena.equals(damagerArena)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onMobTarget(EntityTargetEvent event) {
        if (event.getEntity().hasMetadata("MA_MOB") && event.getTarget() != null && event.getTarget().hasMetadata("MA_MOB")) {
            event.setCancelled(true);
        }
    }
}