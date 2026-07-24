package vn.meowchan12.mobarenav2.arena;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import vn.meowchan12.mobarenav2.MobArenav2;
import vn.meowchan12.mobarenav2.utils.ArenaMath;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MobFactory {

    private final MobArenav2 plugin;
    private final MobAttributeParser attributeParser;
    private final boolean hasMythicMobs;

    public MobFactory(MobArenav2 plugin) {
        this.plugin = plugin;
        this.attributeParser = new MobAttributeParser(plugin);
        this.hasMythicMobs = Bukkit.getPluginManager().getPlugin("MythicMobs") != null;
    }

    // Hex color translation algorithm (&#FFAA00) to Bukkit standard
    private String colorize(String text) {
        if (text == null) return null;
        Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.of("#" + matcher.group(1)).toString());
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString());
    }

    public LivingEntity spawnArenaMob(Arena arena, String mobName, Location loc, ConfigurationSection mobConfig, ConfigurationSection waveConfig) {
        if (loc == null || loc.getWorld() == null) return null;

        Mob spawnedMob = null;
        boolean isCustomBoss = false;

        // CẬP NHẬT: Lấy config Boss từ ConfigManager thay vì BossManager để đồng bộ
        ConfigurationSection bossConfig = plugin.getConfigManager().getBosses().getConfigurationSection(mobName);

        if (bossConfig != null) {
            isCustomBoss = true;
            String entityTypeStr = bossConfig.getString("type", "ZOMBIE").toUpperCase();
            try {
                EntityType type = EntityType.valueOf(entityTypeStr);
                Entity entity = loc.getWorld().spawnEntity(loc, type);
                if (entity instanceof Mob m) {
                    spawnedMob = m;

                    org.bukkit.NamespacedKey bKey = new org.bukkit.NamespacedKey(plugin, "arena_boss_id");
                    spawnedMob.getPersistentDataContainer().set(bKey, org.bukkit.persistence.PersistentDataType.STRING, mobName);

                    // Apply base attributes first
                    attributeParser.applyBossAttributes(spawnedMob, bossConfig);

                    // --- DYNAMIC BOSS HP SCALING ---
                    int playerCount = arena.getActivePlayers().size();
                    double baseHp = bossConfig.getDouble("HP", 100.0);
                    double scaledHp = ArenaMath.calculateBossHealth(baseHp, 1.0, playerCount);

                    // Override the generic max health with the dynamically scaled health
                    if (spawnedMob.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                        spawnedMob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(scaledHp);
                        spawnedMob.setHealth(scaledHp);
                    }
                    // -------------------------------

                    // Fixed color issues using colorize() function
                    String dName = colorize(bossConfig.getString("Boss_displayname", mobName));
                    boolean bBar = bossConfig.getBoolean("Bossbar", false);
                    boolean nTag = bossConfig.getBoolean("nametagshow", false);
                    List<String> skills = bossConfig.getStringList("skill");
                    List<String> enrageSkills = bossConfig.getStringList("enrage-skill");

                    new ArenaBoss(plugin, arena, spawnedMob, dName, bBar, nTag, skills, enrageSkills);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[MobArenav2] Invalid Boss entity type in bosses.yml: " + entityTypeStr);
            }
        }

        // Integration with MythicMobs
        if (!isCustomBoss && hasMythicMobs) {
            MythicMob mythicMob = MythicBukkit.inst().getMobManager().getMythicMob(mobName).orElse(null);
            if (mythicMob != null) {
                ActiveMob activeMob = mythicMob.spawn(BukkitAdapter.adapt(loc), 1);
                if (activeMob != null && activeMob.getEntity().getBukkitEntity() instanceof Mob m) {
                    spawnedMob = m;
                }
            }
        }

        // Vanilla fallback
        if (!isCustomBoss && spawnedMob == null) {
            try {
                EntityType type = EntityType.valueOf(mobName.toUpperCase());
                Entity entity = loc.getWorld().spawnEntity(loc, type);

                if (entity instanceof Mob m) {
                    spawnedMob = m;
                } else if (entity != null) {
                    entity.remove();
                    plugin.getLogger().warning("[MobArenav2] Entity '" + mobName + "' is not a valid combat Mob! Removed.");
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[MobArenav2] Invalid mob type in config: " + mobName);
            }
        }

        // Registration and custom attributes
        if (spawnedMob != null) {
            arena.getActiveMobs().add(spawnedMob);
            if (!isCustomBoss && mobConfig != null && waveConfig != null) {
                attributeParser.applyAttributes(spawnedMob, mobConfig, waveConfig);
            }
        }

        return spawnedMob;
    }
}