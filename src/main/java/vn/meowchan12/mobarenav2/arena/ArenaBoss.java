package vn.meowchan12.mobarenav2.arena;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import vn.meowchan12.mobarenav2.MobArenav2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class ArenaBoss {

    private final MobArenav2 plugin;
    private final Arena arena;
    private final LivingEntity entity;
    private final String displayName;

    private BossBar bossBar;

    private final Map<String, Integer> maxCooldowns = new HashMap<>();
    private final Map<String, Integer> currentCooldowns = new HashMap<>();

    private final List<String> enrageSkills = new ArrayList<>();
    private boolean hasEnraged = false;

    public ArenaBoss(MobArenav2 plugin, Arena arena, LivingEntity entity, String displayName, boolean showBossBar, boolean showNameTag, List<String> skills, List<String> enrageSkillsData) {
        this.plugin = plugin;
        this.arena = arena;
        this.entity = entity;
        this.displayName = displayName != null ? displayName.replace("&", "§") : "Boss";

        if (showNameTag) {
            this.entity.setCustomName(this.displayName);
            this.entity.setCustomNameVisible(true);
        }

        if (showBossBar) {
            this.bossBar = Bukkit.createBossBar(this.displayName, BarColor.RED, BarStyle.SOLID);
            for (Player p : arena.getPlayersAsObjects()) {
                this.bossBar.addPlayer(p);
            }
        }

        // --- THUẬT TOÁN ĐỌC SKILL THÔNG MINH MỚI ---
        if (skills != null) {
            for (String skillData : skills) {
                String[] parts = skillData.split(":");
                String skillId = parts[0].trim();
                int cooldownTicks = 20 * 20; // Mặc định 20 giây nếu không tìm thấy cấu hình

                if (parts.length == 2) {
                    // Nếu user ghi rõ cooldown (VD: earthquake:15)
                    try {
                        cooldownTicks = (int) (Double.parseDouble(parts[1].trim()) * 20);
                    } catch (NumberFormatException ignored) {}
                } else {
                    // Tự động đối chiếu và lấy cooldown từ bossskill.yml
                    ConfigurationSection skillCfg = plugin.getBossManager().getSkillsConfig().getConfigurationSection(skillId);
                    if (skillCfg != null && skillCfg.contains("cooldown")) {
                        cooldownTicks = skillCfg.getInt("cooldown") * 20;
                    }
                }

                maxCooldowns.put(skillId, cooldownTicks);
                // Tạo độ trễ ngẫu nhiên (3-8 giây) cho lần tung chiêu đầu tiên để tránh Boss xả hết skill cùng lúc
                currentCooldowns.put(skillId, ThreadLocalRandom.current().nextInt(60, 160));
            }
        }

        if (enrageSkillsData != null) {
            this.enrageSkills.addAll(enrageSkillsData);
        }

        startTask();
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (entity == null || entity.isDead() || !entity.isValid() || !arena.isRunning()) {
                    cleanup();
                    this.cancel();
                    return;
                }

                double maxHealth = 100.0;
                if (entity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                    maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                }
                double health = Math.max(0.0, entity.getHealth());
                double progress = health / maxHealth;

                if (bossBar != null) {
                    bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                    String hpText = String.format("%.1f", health);
                    String maxHpText = String.format("%.1f", maxHealth);
                    bossBar.setTitle(displayName + " §7- §c" + hpText + " §7/ §c" + maxHpText);
                }

                if (!hasEnraged && progress <= 0.3) {
                    hasEnraged = true;
                    for (String eSkill : enrageSkills) {
                        BossSkillExecutor.castSkill(plugin, arena, entity, eSkill);
                    }
                }

                for (String skillId : currentCooldowns.keySet()) {
                    int left = currentCooldowns.get(skillId);
                    if (left > 0) {
                        currentCooldowns.put(skillId, left - 1);
                    } else {
                        BossSkillExecutor.castSkill(plugin, arena, entity, skillId);
                        currentCooldowns.put(skillId, maxCooldowns.get(skillId));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void cleanup() {
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }
}