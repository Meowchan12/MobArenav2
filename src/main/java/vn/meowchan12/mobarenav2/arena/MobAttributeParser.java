package vn.meowchan12.mobarenav2.arena;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import vn.meowchan12.mobarenav2.MobArenav2;
import vn.meowchan12.mobarenav2.utils.ItemParser;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MobAttributeParser {

    private final MobArenav2 plugin;

    public MobAttributeParser(MobArenav2 plugin) {
        this.plugin = plugin;
    }

    public void applyAttributes(LivingEntity mob, ConfigurationSection mobConfig, ConfigurationSection waveConfig) {
        if (mob == null || !mob.isValid()) return;

        String hpBonusStr = getBonusString("maxhp-bonus", mobConfig, waveConfig);
        String damageBonusStr = getBonusString("damage-bonus", mobConfig, waveConfig);
        String speedBonusStr = getBonusString("movement-speed", mobConfig, waveConfig);
        String explosionBonusStr = getBonusString("explosion-damage", mobConfig, waveConfig);
        String bowSpeedBonusStr = getBonusString("bow-speed", mobConfig, waveConfig);
        String effectStr = getBonusString("effect", mobConfig, waveConfig);

        applyGenericAttribute(mob, Attribute.GENERIC_MAX_HEALTH, hpBonusStr);
        if (hpBonusStr != null) {
            AttributeInstance maxHpAttr = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHpAttr != null) mob.setHealth(maxHpAttr.getValue());
        }

        applyGenericAttribute(mob, Attribute.GENERIC_ATTACK_DAMAGE, damageBonusStr);
        applyGenericAttribute(mob, Attribute.GENERIC_MOVEMENT_SPEED, speedBonusStr);

        applyExplosionBonus(mob, explosionBonusStr);
        applyBowSpeedBonus(mob, bowSpeedBonusStr);

        if (mobConfig != null && mobConfig.contains("equip")) {
            applyEquipment(mob, mobConfig.getStringList("equip"));
        }

        if (effectStr != null && !effectStr.isEmpty()) {
            applyPotionEffects(mob, effectStr);
        }
    }

    public void applyBossAttributes(LivingEntity mob, ConfigurationSection bossConfig) {
        if (mob == null || !mob.isValid() || bossConfig == null) return;

        if (bossConfig.contains("HP") && mob.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            double hp = bossConfig.getDouble("HP");
            mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);
            mob.setHealth(hp);
        }

        if (bossConfig.contains("Damage") && mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(bossConfig.getDouble("Damage"));
        }

        if (bossConfig.contains("Equip")) {
            applyEquipment(mob, bossConfig.getStringList("Equip"));
        }

        if (bossConfig.contains("effect-bonus")) {
            applyPotionEffects(mob, bossConfig.getString("effect-bonus"));
        }
    }

    private String getBonusString(String key, ConfigurationSection mobConfig, ConfigurationSection waveConfig) {
        if (mobConfig != null && mobConfig.contains(key)) return mobConfig.getString(key);
        if (key.equals("effect") && waveConfig != null && waveConfig.contains("general-effect")) return waveConfig.getString("general-effect");
        if (waveConfig != null && waveConfig.contains(key)) return waveConfig.getString(key);
        return null;
    }

    private void applyGenericAttribute(LivingEntity mob, Attribute attribute, String bonusStr) {
        if (bonusStr == null || bonusStr.isEmpty()) return;
        AttributeInstance attrInstance = mob.getAttribute(attribute);
        if (attrInstance == null) return;

        double currentValue = attrInstance.getBaseValue();
        double newValue = currentValue;

        try {
            if (bonusStr.startsWith("x")) {
                newValue = currentValue * Double.parseDouble(bonusStr.replace("x", "").trim());
            } else if (bonusStr.startsWith("+")) {
                newValue = currentValue + Double.parseDouble(bonusStr.replace("+", "").trim());
            }
            attrInstance.setBaseValue(newValue);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("[MobArenav2] Invalid attribute format for " + attribute.name() + ": " + bonusStr);
        }
    }

    private void applyExplosionBonus(LivingEntity mob, String bonusStr) {
        if (bonusStr == null || bonusStr.isEmpty()) return;

        if (mob instanceof Creeper creeper) {
            double currentRadius = creeper.getExplosionRadius();
            double newRadius = currentRadius;

            try {
                if (bonusStr.startsWith("x")) {
                    newRadius = currentRadius * Double.parseDouble(bonusStr.replace("x", "").trim());
                } else if (bonusStr.startsWith("+")) {
                    newRadius = currentRadius + Double.parseDouble(bonusStr.replace("+", "").trim());
                }
                creeper.setExplosionRadius((int) Math.round(newRadius));
            } catch (NumberFormatException ignored) {}
        } else {
            NamespacedKey key = new NamespacedKey(plugin, "explosion_bonus");
            mob.getPersistentDataContainer().set(key, PersistentDataType.STRING, bonusStr);
        }
    }

    private void applyBowSpeedBonus(LivingEntity mob, String bonusStr) {
        if (bonusStr == null || bonusStr.isEmpty()) return;
        NamespacedKey key = new NamespacedKey(plugin, "bow_speed_bonus");
        mob.getPersistentDataContainer().set(key, PersistentDataType.STRING, bonusStr);
    }

    // ==========================================
    // VÁ LỖI: Đồng bộ hóa xử lý đồ với ItemParser
    // ==========================================
    private void applyEquipment(LivingEntity mob, List<String> equips) {
        if (mob.getEquipment() == null) return;

        for (String eqLine : equips) {
            if (eqLine == null || eqLine.isEmpty()) continue;

            // Kiểm tra tay cầm (Offhand)
            boolean isOffhand = eqLine.toUpperCase().contains("OFF_HAND");

            // Xử lý Tỷ lệ rớt (Chance)
            int chance = 100;
            if (eqLine.contains("%")) {
                String[] parts = eqLine.split("%");
                try {
                    chance = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ignored) {}
            }

            // Gieo xúc xắc (RNG)
            if (ThreadLocalRandom.current().nextInt(100) >= chance) continue;

            // Nhờ ItemParser dịch chuỗi thành ItemStack hoàn chỉnh (Bao gồm cả CustomModelData)
            ItemStack item = ItemParser.parseItem(eqLine);
            if (item == null || item.getType() == Material.AIR) continue;

            String matName = item.getType().name();

            // Mặc giáp / Cầm vũ khí
            if (isOffhand) mob.getEquipment().setItemInOffHand(item);
            else if (matName.endsWith("_HELMET")) mob.getEquipment().setHelmet(item);
            else if (matName.endsWith("_CHESTPLATE")) mob.getEquipment().setChestplate(item);
            else if (matName.endsWith("_LEGGINGS")) mob.getEquipment().setLeggings(item);
            else if (matName.endsWith("_BOOTS")) mob.getEquipment().setBoots(item);
            else mob.getEquipment().setItemInMainHand(item);

            // Tắt rớt đồ Vanilla để nhường cho Custom Loot
            mob.getEquipment().setItemInMainHandDropChance(0f);
            mob.getEquipment().setItemInOffHandDropChance(0f);
            mob.getEquipment().setHelmetDropChance(0f);
            mob.getEquipment().setChestplateDropChance(0f);
            mob.getEquipment().setLeggingsDropChance(0f);
            mob.getEquipment().setBootsDropChance(0f);
        }
    }

    private void applyPotionEffects(LivingEntity mob, String effectString) {
        String[] effects = effectString.split(",");
        for (String eff : effects) {
            String[] data = eff.trim().split(":");
            if (data.length == 2) {
                NamespacedKey key = NamespacedKey.minecraft(data[0].toLowerCase());
                PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(key);
                if (type != null) {
                    try {
                        int level = Integer.parseInt(data[1]);
                        mob.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, level, false, false));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }
}