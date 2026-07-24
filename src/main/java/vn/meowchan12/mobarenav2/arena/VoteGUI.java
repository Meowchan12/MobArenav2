package vn.meowchan12.mobarenav2.arena;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import vn.meowchan12.mobarenav2.MobArenav2;

public class VoteGUI {
    public static void open(Player player, MobArenav2 plugin) {
        Inventory gui = Bukkit.createInventory(null, 27, Component.text("§8Vote: Endless Mode?"));

        ItemStack yes = new ItemStack(Material.LIME_WOOL);
        ItemMeta yesMeta = yes.getItemMeta();
        yesMeta.displayName(Component.text("§a§lCONTINUE (Endless Mode)"));
        yes.setItemMeta(yesMeta);

        ItemStack no = new ItemStack(Material.RED_WOOL);
        ItemMeta noMeta = no.getItemMeta();
        noMeta.displayName(Component.text("§c§lSTOP (Claim Rewards)"));
        no.setItemMeta(noMeta);

        gui.setItem(11, yes);
        gui.setItem(15, no);

        player.openInventory(gui);
    }
}