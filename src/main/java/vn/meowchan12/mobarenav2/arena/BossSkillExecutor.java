package vn.meowchan12.mobarenav2.arena;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import vn.meowchan12.mobarenav2.MobArenav2;
import vn.meowchan12.mobarenav2.arena.MobFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BossSkillExecutor {

    public static void castSkill(MobArenav2 plugin, Arena arena, LivingEntity boss, String skillId) {
        if (boss == null || boss.isDead() || !arena.isRunning()) return;

        ConfigurationSection skillConfig = plugin.getBossManager().getSkillsConfig().getConfigurationSection(skillId);
        if (skillConfig == null) {
            plugin.getLogger().warning("[MobArenav2] Boss tried to use unknown skill: " + skillId);
            return;
        }

        String type = skillConfig.getString("type", "").toUpperCase();
        String skillName = skillConfig.getString("display-name", skillId);
        String bossName = boss.getCustomName() != null ? boss.getCustomName() : "Boss";

        // --- CƠ CHẾ ĐỌC CUSTOM MESSAGE TỪ BOSSSKILL.YML ---
        if (skillConfig.contains("message")) {
            String customMsg = skillConfig.getString("message");
            if (customMsg != null && !customMsg.isEmpty()) {
                String formatted = customMsg.replace("%boss%", bossName).replace("%skill%", skillName);
                arena.broadcastMessage(ChatColor.translateAlternateColorCodes('&', formatted));
            }
        } else {
            // Thông báo mặc định nếu không set custom message
            arena.broadcastMessage("§c§l[BOSS] §4" + bossName + " §euses §c" + skillName + "§e!");
        }

        Location bossLoc = boss.getLocation();

        switch (type) {

            // ==========================================
            // V1 SKILLS (OLD SYSTEM) - RETAINED 100%
            // ==========================================
            case "SUPER_ARROW_DAMAGE":
                double saDmg = skillConfig.getDouble("damage", 8.0);
                double saTDmg = skillConfig.getDouble("true-damage-amount", 4.0);
                long activeUntil = System.currentTimeMillis() + (skillConfig.getInt("active-time", 5) * 1000L);

                NamespacedKey saKey = new NamespacedKey(plugin, "ma_super_arrow");
                boss.getPersistentDataContainer().set(saKey, PersistentDataType.STRING, saDmg + ";" + saTDmg + ";" + activeUntil);
                bossLoc.getWorld().spawnParticle(Particle.FLAME, bossLoc.clone().add(0, 2, 0), 30, 0.5, 0.5, 0.5, 0.1);
                break;

            case "ULTIMATE_ARROW_DAMAGE":
                String targets = skillConfig.getString("targets", "all");
                double uDmg = skillConfig.getDouble("damage", 10.0);
                double uTDmg = skillConfig.getDouble("true-damage-amount", 6.0);
                List<String> effects = skillConfig.getStringList("effects");
                String effectStr = String.join(",", effects);

                List<Player> targetPlayers = new ArrayList<>();
                for (Player p : arena.getPlayersAsObjects()) {
                    if (arena.containsLocation(p.getLocation())) {
                        targetPlayers.add(p);
                    }
                }
                Collections.shuffle(targetPlayers);

                int count = targetPlayers.size();
                if (targets.contains("-")) {
                    String[] parts = targets.split("-");
                    int min = Integer.parseInt(parts[0].trim());
                    int max = Integer.parseInt(parts[1].trim());
                    count = ThreadLocalRandom.current().nextInt(min, max + 1);
                } else if (!targets.equals("all")) {
                    try { count = Integer.parseInt(targets); } catch (Exception ignored) {}
                }

                for (int i = 0; i < Math.min(count, targetPlayers.size()); i++) {
                    Player p = targetPlayers.get(i);
                    Location spawnLoc = p.getLocation().add(0, 4, 0);

                    Arrow arrow = p.getWorld().spawn(spawnLoc, Arrow.class);
                    arrow.setVelocity(new Vector(0, -1.5, 0));
                    arrow.setShooter(boss);
                    arrow.setFireTicks(200);

                    NamespacedKey uaKey = new NamespacedKey(plugin, "ma_arrow_type");
                    arrow.getPersistentDataContainer().set(uaKey, PersistentDataType.STRING, "ultimate;" + uDmg + ";" + uTDmg + ";" + effectStr);
                    p.getWorld().spawnParticle(Particle.PORTAL, spawnLoc, 50, 0.5, 0.5, 0.5, 0.1);
                }
                break;

            case "KNOCKBACKER":
                double kbRad = skillConfig.getDouble("radius", 6.0);
                double kbDmg = skillConfig.getDouble("damage", 5.0);
                double kbPower = skillConfig.getDouble("knockback-power", 2.0);
                double kbHeight = skillConfig.getDouble("knockback-height", 1.2);
                List<String> kbEffects = skillConfig.getStringList("effects");

                bossLoc.getWorld().spawnParticle(Particle.SONIC_BOOM, bossLoc, 1);
                bossLoc.getWorld().playSound(bossLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.0f);

                for (Entity e : boss.getNearbyEntities(kbRad, kbRad, kbRad)) {
                    if (e instanceof Player p && arena.getActivePlayers().contains(p.getUniqueId()) && arena.containsLocation(p.getLocation())) {
                        p.damage(kbDmg, boss);
                        Vector dir = p.getLocation().toVector().subtract(bossLoc.toVector()).normalize();
                        p.setVelocity(dir.multiply(kbPower).setY(kbHeight));

                        for (String eff : kbEffects) {
                            String[] data = eff.trim().split(":");
                            if (data.length >= 2) {
                                PotionEffectType pType = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(data[0].toLowerCase()));
                                if (pType != null) {
                                    p.addPotionEffect(new PotionEffect(pType, Integer.parseInt(data[1]), data.length > 2 ? Integer.parseInt(data[2]) : 0));
                                }
                            }
                        }
                    }
                }
                break;

            case "DEATH_MARK":
                double chance = skillConfig.getDouble("chance", 50.0);
                if (ThreadLocalRandom.current().nextDouble(100.0) <= chance) {
                    List<Player> dPlayers = new ArrayList<>();
                    for (Player p : arena.getPlayersAsObjects()) {
                        if (arena.containsLocation(p.getLocation())) dPlayers.add(p);
                    }

                    if (!dPlayers.isEmpty()) {
                        Player target = dPlayers.get(ThreadLocalRandom.current().nextInt(dPlayers.size()));
                        long dDuration = skillConfig.getInt("duration", 10) * 1000L;
                        long expiry = System.currentTimeMillis() + dDuration;

                        NamespacedKey dmKey = new NamespacedKey(plugin, "ma_death_mark");
                        target.getPersistentDataContainer().set(dmKey, PersistentDataType.LONG, expiry);

                        target.sendMessage("§4☠ YOU HAVE BEEN MARKED FOR DEATH! Totems disabled for " + (dDuration/1000) + "s!");
                        target.getWorld().spawnParticle(Particle.SCULK_SOUL, target.getLocation().add(0, 2, 0), 30, 0.5, 0.5, 0.5, 0.05);
                        target.playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
                    }
                }
                break;

            case "FREEZE":
                int fTime = skillConfig.getInt("freeze-time", 3);
                double hpLoss = skillConfig.getDouble("hp-loss-percent", 35.0);
                List<String> fEffects = skillConfig.getStringList("after-effects");

                bossLoc.getWorld().playSound(bossLoc, Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 2.0f);

                for (Player p : arena.getPlayersAsObjects()) {
                    if (!arena.containsLocation(p.getLocation())) continue;

                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, fTime * 20, 255, false, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, fTime * 20, 128, false, false));
                    p.sendMessage("§b❄ YOU HAVE BEEN FROZEN!");
                    p.getWorld().spawnParticle(Particle.SNOWFLAKE, p.getLocation(), 50, 1.0, 1.0, 1.0, 0.0);

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (p.isOnline() && arena.getActivePlayers().contains(p.getUniqueId())) {
                            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.5f, 1.0f);

                            AttributeInstance maxHpAttr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                            double maxHp = (maxHpAttr != null) ? maxHpAttr.getValue() : 20.0;
                            double loss = maxHp * (hpLoss / 100.0);

                            double newHp = p.getHealth() - loss;
                            if (newHp <= 0) p.damage(9999.0);
                            else p.setHealth(newHp);

                            for (String eff : fEffects) {
                                String[] data = eff.trim().split(":");
                                if (data.length >= 2) {
                                    PotionEffectType pType = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(data[0].toLowerCase()));
                                    if (pType != null) {
                                        p.addPotionEffect(new PotionEffect(pType, Integer.parseInt(data[1]), data.length > 2 ? Integer.parseInt(data[2]) : 0));
                                    }
                                }
                            }
                        }
                    }, fTime * 20L);
                }
                break;

            case "AREA_DAMAGE":
                double radius = skillConfig.getDouble("radius", 5.0);
                double damage = skillConfig.getDouble("damage", 5.0);

                bossLoc.getWorld().spawnParticle(Particle.EXPLOSION, bossLoc, 5, 1.0, 0.5, 1.0, 0.0);
                bossLoc.getWorld().playSound(bossLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);

                for (Entity e : boss.getNearbyEntities(radius, radius, radius)) {
                    if (e instanceof Player p && arena.getActivePlayers().contains(p.getUniqueId()) && arena.containsLocation(p.getLocation())) {
                        p.damage(damage, boss);
                    }
                }
                break;

            case "LIGHTNING":
                int lTargets = skillConfig.getInt("targets", 3);
                List<Player> players = new ArrayList<>();
                for (Player p : arena.getPlayersAsObjects()) {
                    if (arena.containsLocation(p.getLocation())) players.add(p);
                }
                Collections.shuffle(players);

                for (int i = 0; i < lTargets && i < players.size(); i++) {
                    Player target = players.get(i);
                    target.getWorld().strikeLightning(target.getLocation());
                    target.damage(skillConfig.getDouble("damage", 4.0), boss);
                }
                break;

            case "PULL":
                double pRadius = skillConfig.getDouble("radius", 10.0);
                double pStrength = skillConfig.getDouble("strength", 1.0);

                bossLoc.getWorld().playSound(bossLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 0.5f);
                bossLoc.getWorld().spawnParticle(Particle.PORTAL, bossLoc, 50, 2.0, 2.0, 2.0, 1.0);

                for (Entity e : boss.getNearbyEntities(pRadius, pRadius, pRadius)) {
                    if (e instanceof Player p && arena.getActivePlayers().contains(p.getUniqueId()) && arena.containsLocation(p.getLocation())) {
                        Vector direction = bossLoc.toVector().subtract(p.getLocation().toVector()).normalize();
                        p.setVelocity(direction.multiply(pStrength));
                    }
                }
                break;

            case "SUMMON":
                String minionId = skillConfig.getString("minion-id", "zombie");
                int amount = skillConfig.getInt("amount", 3);
                MobFactory mobFactory = new MobFactory(plugin);

                bossLoc.getWorld().spawnParticle(Particle.SMOKE, bossLoc, 30, 1.0, 1.0, 1.0, 0.1);
                bossLoc.getWorld().playSound(bossLoc, Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);

                for (int i = 0; i < amount; i++) {
                    Location spawnLoc = bossLoc.clone().add((Math.random() - 0.5) * 4, 0, (Math.random() - 0.5) * 4);
                    mobFactory.spawnArenaMob(arena, minionId, spawnLoc, null, null);
                }
                break;

            case "METEOR_SHOWER":
                int mTargets = skillConfig.getInt("targets", 2);
                List<Player> targetList = new ArrayList<>();
                for (Player p : arena.getPlayersAsObjects()) {
                    if (arena.containsLocation(p.getLocation())) targetList.add(p);
                }
                Collections.shuffle(targetList);

                for (int i = 0; i < Math.min(mTargets, targetList.size()); i++) {
                    Player p = targetList.get(i);
                    Location startLoc = p.getLocation().add(0, 15, 0);
                    Vector direction = new Vector(0, -1, 0);

                    org.bukkit.entity.Fireball fireball = p.getWorld().spawn(startLoc, org.bukkit.entity.Fireball.class);
                    fireball.setShooter(boss);
                    fireball.setDirection(direction);
                    fireball.setYield((float) skillConfig.getDouble("explosion-power", 2.0));
                }
                break;

            case "TELEPORT_STRIKE":
                Player furthest = null;
                double maxDist = 0;
                for (Player p : arena.getPlayersAsObjects()) {
                    if (!arena.containsLocation(p.getLocation())) continue;
                    double dist = p.getLocation().distanceSquared(bossLoc);
                    if (dist > maxDist) {
                        maxDist = dist;
                        furthest = p;
                    }
                }

                if (furthest != null) {
                    bossLoc.getWorld().spawnParticle(Particle.PORTAL, bossLoc, 50, 1.0, 2.0, 1.0, 1.0);

                    Vector dir = furthest.getLocation().getDirection().setY(0).normalize();
                    Location behind = furthest.getLocation().subtract(dir.multiply(2));

                    boss.teleport(behind);
                    boss.getWorld().playSound(behind, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    boss.getWorld().spawnParticle(Particle.PORTAL, behind, 50, 1.0, 2.0, 1.0, 1.0);

                    furthest.damage(skillConfig.getDouble("damage", 10.0), boss);
                    furthest.setVelocity(dir.multiply(1.5));
                }
                break;

            case "ENRAGE":
                bossLoc.getWorld().playSound(bossLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
                bossLoc.getWorld().spawnParticle(Particle.FLAME, bossLoc, 100, 1.0, 2.0, 1.0, 0.2);
                boss.setGlowing(true);

                String buffs = skillConfig.getString("effects", "speed:3, strength:2");
                String[] enrageEffects = buffs.split(",");
                for (String eff : enrageEffects) {
                    String[] data = eff.trim().split(":");
                    if (data.length == 2) {
                        NamespacedKey key = NamespacedKey.minecraft(data[0].toLowerCase());
                        PotionEffectType pType = Registry.POTION_EFFECT_TYPE.get(key);
                        if (pType != null) {
                            boss.addPotionEffect(new PotionEffect(pType, Integer.MAX_VALUE, Integer.parseInt(data[1]), false, false));
                        }
                    }
                }
                break;

            case "INVULNERABLE":
                break;

            // ==========================================
            // V2 SKILLS (NEW SYSTEM) - 9 ADVANCED SKILLS
            // ==========================================

            case "MAGNETIC_PULL":
                double mpRadius = skillConfig.getDouble("radius", 12.0);
                double mpStrength = skillConfig.getDouble("pull-strength", 1.5);

                bossLoc.getWorld().playSound(bossLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.5f);
                bossLoc.getWorld().spawnParticle(Particle.PORTAL, bossLoc, 100, 3.0, 3.0, 3.0, 0.5);

                for (Player p : arena.getPlayersAsObjects()) {
                    if (arena.containsLocation(p.getLocation()) && p.getLocation().distance(bossLoc) <= mpRadius) {
                        Vector dir = bossLoc.toVector().subtract(p.getLocation().toVector()).normalize();
                        p.setVelocity(dir.multiply(mpStrength));
                        applyEffects(p, skillConfig.getStringList("effects"));
                    }
                }
                break;

            case "EARTHQUAKE_SMASH":
                double eqRadius = skillConfig.getDouble("radius", 8.0);
                double eqJump = skillConfig.getDouble("jump-velocity", 1.5);
                double eqKnockup = skillConfig.getDouble("knockup-power", 1.2);
                double eqDamage = skillConfig.getDouble("damage", 10.0);

                boss.setVelocity(new Vector(0, eqJump, 0));

                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (boss.isDead() || !arena.isRunning() || ticks++ > 40) { this.cancel(); return; }

                        if (ticks > 5 && boss.isOnGround()) {
                            Location landLoc = boss.getLocation();
                            landLoc.getWorld().playSound(landLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.8f);
                            landLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, landLoc, 1);

                            for (Player p : arena.getPlayersAsObjects()) {
                                if (arena.containsLocation(p.getLocation()) && p.getLocation().distance(landLoc) <= eqRadius) {
                                    p.damage(eqDamage, boss);
                                    p.setVelocity(new Vector(0, eqKnockup, 0));
                                }
                            }
                            this.cancel();
                        }
                    }
                }.runTaskTimer(plugin, 1L, 1L);
                break;

            case "TOXIC_NOVA":
                double maxNovaRadius = skillConfig.getDouble("max-radius", 10.0);
                double novaSpeed = skillConfig.getDouble("expansion-speed", 0.5);
                double novaDmg = skillConfig.getDouble("damage-per-tick", 2.0);
                List<String> novaEffects = skillConfig.getStringList("effects");

                new BukkitRunnable() {
                    double currentRadius = 1.0;
                    @Override
                    public void run() {
                        if (boss.isDead() || !arena.isRunning() || currentRadius > maxNovaRadius) { this.cancel(); return; }

                        Location center = boss.getLocation();
                        for (double theta = 0; theta < 2 * Math.PI; theta += Math.PI / 16) {
                            double x = currentRadius * Math.cos(theta);
                            double z = currentRadius * Math.sin(theta);
                            center.getWorld().spawnParticle(Particle.WITCH, center.clone().add(x, 0.5, z), 1, 0, 0, 0, 0);
                        }

                        for (Player p : arena.getPlayersAsObjects()) {
                            if (arena.containsLocation(p.getLocation()) && p.getLocation().distance(center) <= currentRadius) {
                                p.damage(novaDmg, boss);
                                applyEffects(p, novaEffects);
                            }
                        }
                        currentRadius += novaSpeed;
                    }
                }.runTaskTimer(plugin, 0L, 2L);
                break;

            case "MINION_CALL":
                String mType = skillConfig.getString("mob-type", "ZOMBIE");
                int mAmount = skillConfig.getInt("amount", 4);
                int mLifespan = skillConfig.getInt("lifespan", 15);

                bossLoc.getWorld().playSound(bossLoc, Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.5f);
                MobFactory mFactory = new MobFactory(plugin);

                for (int i = 0; i < mAmount; i++) {
                    Location spawnLoc = bossLoc.clone().add((Math.random() - 0.5) * 4, 0, (Math.random() - 0.5) * 4);
                    LivingEntity minion = mFactory.spawnArenaMob(arena, mType.toLowerCase(), spawnLoc, null, null);
                    if (minion != null) {
                        applyEffects(minion, skillConfig.getStringList("minion-buffs"));
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (minion.isValid() && !minion.isDead()) {
                                minion.getWorld().spawnParticle(Particle.LARGE_SMOKE, minion.getLocation(), 10, 0.5, 0.5, 0.5, 0);
                                minion.remove();
                                arena.getActiveMobs().remove(minion);
                            }
                        }, mLifespan * 20L);
                    }
                }
                break;

            case "DASH_AND_RANGED":
                double dashPwr = skillConfig.getDouble("dash.power", 2.5);
                int invulnDur = skillConfig.getInt("dash.invulnerable-duration", 3);
                int rDuration = skillConfig.getInt("ranged-stance.duration", 10);
                double rDist = skillConfig.getDouble("ranged-stance.max-target-distance", 20.0);
                long rInterval = (long) (skillConfig.getDouble("ranged-stance.attack-interval", 1.0) * 20L);

                Vector dashDir = boss.getLocation().getDirection().setY(0.2).normalize().multiply(dashPwr);
                boss.setVelocity(dashDir);

                NamespacedKey invKey = new NamespacedKey(plugin, "ma_invulnerable");
                boss.getPersistentDataContainer().set(invKey, PersistentDataType.LONG, System.currentTimeMillis() + (invulnDur * 1000L));
                bossLoc.getWorld().playSound(bossLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 1.5f);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (boss.isDead() || !arena.isRunning()) return;

                    boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, rDuration * 20, 100, false, false));
                    boss.getWorld().spawnParticle(Particle.ENCHANTED_HIT, boss.getLocation(), 50, 1, 1, 1, 0.1);

                    new BukkitRunnable() {
                        int ticks = 0;
                        @Override
                        public void run() {
                            if (boss.isDead() || !arena.isRunning() || ticks >= (rDuration * 20)) {
                                this.cancel();
                                return;
                            }

                            if (ticks % rInterval == 0) {
                                Player target = getNearestPlayer(arena, boss.getLocation(), rDist);
                                if (target != null) {
                                    Location eye = boss.getEyeLocation();
                                    Location tEye = target.getEyeLocation();

                                    drawLaser(eye, tEye, Particle.DUST, 255, 0, 0);
                                    target.damage(5.0, boss);
                                    boss.getWorld().playSound(eye, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 2.0f);
                                }
                            }
                            ticks++;
                        }
                    }.runTaskTimer(plugin, 0L, 1L);

                }, 20L);
                break;

            case "INVIS_TELEPORTER":
                int iDur = skillConfig.getInt("duration", 12);
                int tInterval = skillConfig.getInt("teleport-interval", 3) * 20;
                double tMin = skillConfig.getDouble("teleport-distance.min", 4.0);
                double tMax = skillConfig.getDouble("teleport-distance.max", 8.0);

                boss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, iDur * 20, 1, false, false));

                new BukkitRunnable() {
                    int elapsed = 0;
                    @Override
                    public void run() {
                        if (boss.isDead() || !arena.isRunning() || elapsed >= (iDur * 20)) { this.cancel(); return; }

                        if (elapsed % tInterval == 0) {
                            Player target = getNearestPlayer(arena, boss.getLocation(), 50.0);
                            if (target != null) {
                                double angle = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);
                                double dist = ThreadLocalRandom.current().nextDouble(tMin, tMax);
                                Location tpLoc = target.getLocation().clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

                                if (!tpLoc.getBlock().getType().isSolid()) {
                                    boss.getWorld().spawnParticle(Particle.SMOKE, boss.getLocation(), 20, 0.5, 1, 0.5, 0);
                                    boss.teleport(tpLoc);
                                    boss.getWorld().playSound(tpLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                                }
                            }
                        }
                        elapsed += 5;
                    }
                }.runTaskTimer(plugin, 0L, 5L);
                break;

            case "SPAMMER_BOW":
                int sbDur = skillConfig.getInt("duration", 6);
                long sbInterval = (long) (skillConfig.getDouble("attack-interval", 1.5) * 20L);
                boolean hitscan = skillConfig.getBoolean("guaranteed-hit", true);
                double sbKnockback = skillConfig.getDouble("knockback-force", 1.2);
                double debuffChance = skillConfig.getDouble("debuff.chance", 15.0);
                List<String> sbEffects = skillConfig.getStringList("debuff.effects");

                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (boss.isDead() || !arena.isRunning() || ticks >= (sbDur * 20)) { this.cancel(); return; }

                        if (ticks % sbInterval == 0) {
                            boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_SKELETON_SHOOT, 1.0f, 1.2f);
                            for (Player p : arena.getPlayersAsObjects()) {
                                if (arena.containsLocation(p.getLocation())) {
                                    if (hitscan) {
                                        drawLaser(boss.getEyeLocation(), p.getEyeLocation(), Particle.CRIT, 0, 0, 0);
                                        p.damage(4.0, boss);
                                        Vector kbDir = p.getLocation().toVector().subtract(boss.getLocation().toVector()).normalize();
                                        p.setVelocity(kbDir.multiply(sbKnockback).setY(0.5));

                                        if (ThreadLocalRandom.current().nextDouble(100.0) <= debuffChance) {
                                            applyEffects(p, sbEffects);
                                        }
                                    } else {
                                        Arrow arrow = boss.launchProjectile(Arrow.class);
                                        Vector dir = p.getEyeLocation().toVector().subtract(boss.getEyeLocation().toVector()).normalize();
                                        arrow.setVelocity(dir.multiply(2.0));
                                    }
                                }
                            }
                        }
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
                break;

            case "SAITAMA_PUNCH":
                long spDur = skillConfig.getInt("duration", 5) * 1000L;
                double trueDmgAmount = skillConfig.getDouble("true-damage", 5.0);

                NamespacedKey sKey = new NamespacedKey(plugin, "ma_saitama_punch");
                boss.getPersistentDataContainer().set(sKey, PersistentDataType.STRING, trueDmgAmount + ";" + (System.currentTimeMillis() + spDur));

                boss.getWorld().playSound(boss.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 0.5f);
                boss.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, boss.getLocation().add(0, 2, 0), 10, 0.5, 0.5, 0.5, 0);
                break;

            case "LIFESTEAL_AURA":
                long lsDur = skillConfig.getInt("duration", 10) * 1000L;
                double minLs = skillConfig.getDouble("heal-percent.min", 1.0);
                double maxLs = skillConfig.getDouble("heal-percent.max", 1.5);

                NamespacedKey lKey = new NamespacedKey(plugin, "ma_lifesteal");
                boss.getPersistentDataContainer().set(lKey, PersistentDataType.STRING, minLs + ";" + maxLs + ";" + (System.currentTimeMillis() + lsDur));

                boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 2.0f);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (boss.isDead() || ticks >= (lsDur / 50)) { this.cancel(); return; }
                        boss.getWorld().spawnParticle(Particle.HEART, boss.getLocation().add(0, 1, 0), 1, 0.5, 0.5, 0.5, 0);
                        ticks += 10;
                    }
                }.runTaskTimer(plugin, 0L, 10L);
                break;

            default:
                plugin.getLogger().warning("[MobArenav2] Unknown Boss Skill type: " + type);
                break;
        }
    }

    // ==========================================
    // UTILITY METHODS FOR NEW SKILLS
    // ==========================================

    private static void applyEffects(LivingEntity entity, List<String> effects) {
        if (effects == null || effects.isEmpty()) return;
        for (String eff : effects) {
            String[] data = eff.trim().split(":");
            if (data.length >= 2) {
                PotionEffectType pType = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(data[0].toLowerCase()));
                if (pType != null) {
                    int duration = Integer.parseInt(data[1]);
                    int amp = data.length > 2 ? Integer.parseInt(data[2]) : 0;
                    entity.addPotionEffect(new PotionEffect(pType, duration, amp, false, false));
                }
            }
        }
    }

    private static Player getNearestPlayer(Arena arena, Location loc, double maxDistance) {
        Player nearest = null;
        double minDistSq = maxDistance * maxDistance;
        for (Player p : arena.getPlayersAsObjects()) {
            if (arena.containsLocation(p.getLocation())) {
                double distSq = p.getLocation().distanceSquared(loc);
                if (distSq <= minDistSq) {
                    minDistSq = distSq;
                    nearest = p;
                }
            }
        }
        return nearest;
    }

    private static void drawLaser(Location start, Location end, Particle particle, int r, int g, int b) {
        double distance = start.distance(end);
        Vector direction = end.toVector().subtract(start.toVector()).normalize().multiply(0.5);
        Location current = start.clone();

        for (double d = 0; d < distance; d += 0.5) {
            current.add(direction);
            if (particle == Particle.DUST) {
                Particle.DustOptions dust = new Particle.DustOptions(org.bukkit.Color.fromRGB(r, g, b), 1.0f);
                current.getWorld().spawnParticle(particle, current, 1, 0, 0, 0, 0, dust);
            } else {
                current.getWorld().spawnParticle(particle, current, 1, 0, 0, 0, 0);
            }
        }
    }
}