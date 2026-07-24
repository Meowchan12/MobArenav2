package vn.meowchan12.mobarenav2.listener;

import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import vn.meowchan12.mobarenav2.MobArenav2;
import vn.meowchan12.mobarenav2.arena.Arena;
import vn.meowchan12.mobarenav2.arena.ArenaPlayerProfile;

import java.util.concurrent.ThreadLocalRandom;

public class ArenaMobCombatListener implements Listener {

    private final MobArenav2 plugin;
    private final NamespacedKey bowSpeedKey;
    private final NamespacedKey explosionKey;

    // --- NEW: BOSS SKILLS KEYS ---
    private final NamespacedKey saitamaKey;
    private final NamespacedKey lifestealKey;
    private final NamespacedKey invulnKey;

    public ArenaMobCombatListener(MobArenav2 plugin) {
        this.plugin = plugin;
        this.bowSpeedKey = new NamespacedKey(plugin, "bow_speed_bonus");
        this.explosionKey = new NamespacedKey(plugin, "explosion_bonus");

        // Init Boss Skill Keys
        this.saitamaKey = new NamespacedKey(plugin, "ma_saitama_punch");
        this.lifestealKey = new NamespacedKey(plugin, "ma_lifesteal");
        this.invulnKey = new NamespacedKey(plugin, "ma_invulnerable");
    }

    // --- RETAINED: OLD BOW LOGIC ---
    @EventHandler
    public void onMobShootBow(EntityShootBowEvent event) {
        LivingEntity shooter = event.getEntity();
        Entity projectile = event.getProjectile();

        NamespacedKey saKey = new NamespacedKey(plugin, "ma_super_arrow");
        if (shooter.getPersistentDataContainer().has(saKey, PersistentDataType.STRING)) {
            String data = shooter.getPersistentDataContainer().get(saKey, PersistentDataType.STRING);
            if (data != null) {
                String[] parts = data.split(";");
                long expiry = Long.parseLong(parts[2]);

                if (System.currentTimeMillis() <= expiry) {
                    NamespacedKey arrowKey = new NamespacedKey(plugin, "ma_arrow_type");
                    projectile.getPersistentDataContainer().set(arrowKey, PersistentDataType.STRING, "super;" + parts[0] + ";" + parts[1]);
                    projectile.setVisualFire(true);
                } else {
                    shooter.getPersistentDataContainer().remove(saKey);
                }
            }
        }

        if (shooter.getPersistentDataContainer().has(bowSpeedKey, PersistentDataType.STRING)) {
            String bonusStr = shooter.getPersistentDataContainer().get(bowSpeedKey, PersistentDataType.STRING);
            if (bonusStr == null) return;

            Vector velocity = projectile.getVelocity();
            double currentSpeed = velocity.length();
            double newSpeed = currentSpeed;

            try {
                if (bonusStr.startsWith("x")) {
                    newSpeed = currentSpeed * Double.parseDouble(bonusStr.replace("x", "").trim());
                } else if (bonusStr.startsWith("+")) {
                    newSpeed = currentSpeed + Double.parseDouble(bonusStr.replace("+", "").trim());
                }
                projectile.setVelocity(velocity.normalize().multiply(newSpeed));
            } catch (NumberFormatException ignored) {}
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        // =========================================================
        // 1. PLAYER TAKES DAMAGE FROM BOSS OR MOBS
        // =========================================================
        if (event.getEntity() instanceof Player p) {
            Arena arena = plugin.getArenaManager().getArenaByPlayer(p);
            if (arena != null && arena.isRunning() && !arena.getSpectators().contains(p.getUniqueId())) {
                ArenaPlayerProfile profile = arena.getPlayerProfiles().get(p.getUniqueId());

                // --- RETAINED: PLAYER DODGE ALGORITHM ---
                double dodgeChance = profile.getUpgradeValue("DODGE", plugin);
                if (dodgeChance > 0 && ThreadLocalRandom.current().nextDouble(100.0) <= dodgeChance) {
                    event.setCancelled(true);
                    p.sendMessage("§a§lDODGED!");
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 2.0f);
                    return;
                }

                // --- NEW: BOSS MELEE PASSIVE SKILLS ---
                LivingEntity damager = null;
                if (event.getDamager() instanceof LivingEntity) damager = (LivingEntity) event.getDamager();
                else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof LivingEntity) damager = (LivingEntity) proj.getShooter();

                if (damager != null) {
                    // SAITAMA PUNCH (TRUE DAMAGE)
                    if (damager.getPersistentDataContainer().has(saitamaKey, PersistentDataType.STRING)) {
                        String data = damager.getPersistentDataContainer().get(saitamaKey, PersistentDataType.STRING);
                        if (data != null) {
                            String[] parts = data.split(";");
                            double trueDmg = Double.parseDouble(parts[0]);
                            long expiry = Long.parseLong(parts[1]);

                            if (System.currentTimeMillis() <= expiry) {
                                // Apply true damage ignoring armor
                                double newHp = p.getHealth() - trueDmg;
                                if (newHp <= 0) p.damage(9999.0);
                                else p.setHealth(newHp);

                                p.getWorld().spawnParticle(Particle.EXPLOSION, p.getLocation().add(0, 1, 0), 1);
                                p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
                            } else {
                                damager.getPersistentDataContainer().remove(saitamaKey);
                            }
                        }
                    }

                    // LIFESTEAL AURA
                    if (damager.getPersistentDataContainer().has(lifestealKey, PersistentDataType.STRING)) {
                        String data = damager.getPersistentDataContainer().get(lifestealKey, PersistentDataType.STRING);
                        if (data != null) {
                            String[] parts = data.split(";");
                            double minPercent = Double.parseDouble(parts[0]);
                            double maxPercent = Double.parseDouble(parts[1]);
                            long expiry = Long.parseLong(parts[2]);

                            if (System.currentTimeMillis() <= expiry) {
                                double healPercent = ThreadLocalRandom.current().nextDouble(minPercent, maxPercent);
                                AttributeInstance maxHpAttr = damager.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                                double maxHp = (maxHpAttr != null) ? maxHpAttr.getValue() : 100.0;
                                double healAmount = maxHp * (healPercent / 100.0);

                                damager.setHealth(Math.min(damager.getHealth() + healAmount, maxHp));
                                damager.getWorld().spawnParticle(Particle.HEART, damager.getLocation().add(0, 2, 0), 3, 0.5, 0.5, 0.5, 0);
                            } else {
                                damager.getPersistentDataContainer().remove(lifestealKey);
                            }
                        }
                    }
                }
            }

            // --- RETAINED: OLD BOSS ARROW PROCESSING ---
            if (event.getDamager() instanceof Projectile proj) {
                NamespacedKey arrowKey = new NamespacedKey(plugin, "ma_arrow_type");
                if (proj.getPersistentDataContainer().has(arrowKey, PersistentDataType.STRING)) {
                    String data = proj.getPersistentDataContainer().get(arrowKey, PersistentDataType.STRING);
                    if (data == null) return;

                    String[] parts = data.split(";");
                    String type = parts[0];
                    double dmg = Double.parseDouble(parts[1]);
                    double trueDmg = Double.parseDouble(parts[2]);

                    event.setDamage(dmg);

                    if (trueDmg > 0) {
                        double newHp = p.getHealth() - trueDmg;
                        if (newHp <= 0) p.damage(9999.0);
                        else p.setHealth(newHp);
                    }

                    if (type.equals("ultimate") && parts.length > 3) {
                        String effectStr = parts[3];
                        if (!effectStr.isEmpty()) {
                            for (String eff : effectStr.split(",")) {
                                String[] eData = eff.trim().split(":");
                                if (eData.length >= 2) {
                                    PotionEffectType pType = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(eData[0].toLowerCase()));
                                    if (pType != null) {
                                        p.addPotionEffect(new PotionEffect(pType, Integer.parseInt(eData[1]), eData.length > 2 ? Integer.parseInt(eData[2]) : 0));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================
        // 2. PLAYER DEALS DAMAGE TO BOSS OR MOBS
        // =========================================================
        if (event.getDamager() instanceof Player attacker) {
            Arena arena = plugin.getArenaManager().getArenaByPlayer(attacker);
            if (arena != null && arena.isRunning() && !arena.getSpectators().contains(attacker.getUniqueId())) {

                // --- NEW: BOSS INVULNERABLE CHECK (DASH PHASE) ---
                if (event.getEntity() instanceof LivingEntity target) {
                    if (target.getPersistentDataContainer().has(invulnKey, PersistentDataType.LONG)) {
                        long expiry = target.getPersistentDataContainer().get(invulnKey, PersistentDataType.LONG);
                        if (System.currentTimeMillis() <= expiry) {
                            event.setCancelled(true);
                            attacker.playSound(attacker.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
                            return;
                        } else {
                            target.getPersistentDataContainer().remove(invulnKey);
                        }
                    }
                }

                ArenaPlayerProfile profile = arena.getPlayerProfiles().get(attacker.getUniqueId());

                // --- RETAINED: PLAYER CRITICAL STRIKE ---
                double critChance = profile.getUpgradeValue("CRITICAL_STRIKE", plugin);
                if (critChance > 0 && ThreadLocalRandom.current().nextDouble(100.0) <= critChance) {
                    event.setDamage(event.getDamage() * 2.0);
                    attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
                    attacker.getWorld().spawnParticle(Particle.CRIT, event.getEntity().getLocation().add(0, 1, 0), 15);
                }

                // --- RETAINED: PLAYER LIFESTEAL ---
                double lifestealChance = profile.getUpgradeValue("LIFESTEAL", plugin);
                if (lifestealChance > 0 && ThreadLocalRandom.current().nextDouble(100.0) <= lifestealChance) {
                    double maxHp = (attacker.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) ? attacker.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() : 20.0;
                    attacker.setHealth(Math.min(attacker.getHealth() + 1.0, maxHp));
                    attacker.getWorld().spawnParticle(Particle.HEART, attacker.getLocation().add(0, 2, 0), 1);
                }
            }
        }
    }

    // --- RETAINED: OLD EXPLOSION LOGIC ---
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity explosive = event.getEntity();
        LivingEntity shooter = null;

        if (explosive instanceof Projectile proj && proj.getShooter() instanceof LivingEntity) {
            shooter = (LivingEntity) proj.getShooter();
        }

        if (shooter != null && shooter.getPersistentDataContainer().has(explosionKey, PersistentDataType.STRING)) {
            String bonusStr = shooter.getPersistentDataContainer().get(explosionKey, PersistentDataType.STRING);
            if (bonusStr == null) return;

            float currentYield = event.getYield();
            float newYield = currentYield;

            try {
                if (bonusStr.startsWith("x")) {
                    newYield = currentYield * Float.parseFloat(bonusStr.replace("x", "").trim());
                } else if (bonusStr.startsWith("+")) {
                    newYield = currentYield + Float.parseFloat(bonusStr.replace("+", "").trim());
                }
                event.setYield(newYield);
            } catch (NumberFormatException ignored) {}
        }
    }
}