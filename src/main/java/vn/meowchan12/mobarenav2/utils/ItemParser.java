package vn.meowchan12.mobarenav2.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ItemParser {

    public static ItemStack parseItem(String itemString) {
        if (itemString == null || itemString.isEmpty()) return null;

        String[] parts = itemString.trim().split(" ");
        if (parts.length == 0) return null;

        String[] matAmount = parts[0].split(":");
        String materialName = matAmount[0].toUpperCase().replace("OFF_HAND", "").trim();
        Material material = Material.matchMaterial(materialName);

        if (material == null) {
            return new ItemStack(Material.DIRT);
        }

        int amount = 1;
        if (matAmount.length >= 2) {
            try { amount = Integer.parseInt(matAmount[1]); } catch (NumberFormatException ignored) {}
        }

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].toLowerCase().trim();

            if (part.startsWith("%")) continue;

            if (part.matches("\\d+")) {
                try {
                    meta.setCustomModelData(Integer.parseInt(part));
                } catch (NumberFormatException ignored) {}
                continue;
            }

            if ((material == Material.POTION || material == Material.SPLASH_POTION || material == Material.LINGERING_POTION)
                    && i == 1 && parts.length > 1) {

                String[] potData = parts[1].split(":");
                if (potData.length >= 2) {
                    PotionEffectType effectType = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(potData[0].toLowerCase()));
                    int level = 1;
                    try { level = Integer.parseInt(potData[1]); } catch (Exception ignored) {}

                    if (effectType != null && meta instanceof PotionMeta potionMeta) {
                        potionMeta.addCustomEffect(new PotionEffect(effectType, 1, level - 1), true);
                    }
                }
                break;
            }

            // [FIX LỖI TẠI ĐÂY] Tách chuỗi bằng dấu ";" trước để lấy danh sách nhiều enchant
            String[] enchantsList = part.split(";");
            for (String enchString : enchantsList) {
                String[] enchData = enchString.split(":");
                if (enchData.length == 2) {
                    Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchData[0]));
                    if (enchantment != null) {
                        try {
                            int level = Integer.parseInt(enchData[1]);
                            meta.addEnchant(enchantment, level, true);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        item.setItemMeta(meta);
        return item;
    }
}