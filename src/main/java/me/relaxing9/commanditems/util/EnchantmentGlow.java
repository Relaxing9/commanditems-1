package me.relaxing9.commanditems.util;

import java.lang.reflect.Field;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EnchantmentGlow extends EnchantmentWrapper {

    private static Enchantment glow;

    public EnchantmentGlow() {
        super("enchantment_glow");
    }
    
    @SuppressWarnings({"setAccessible"})
    public static Enchantment getGlow() {
        if (glow != null)
            return glow;
        else if ((glow = Enchantment.WATER_WORKER) != null)
            return glow;

        try {
            Field f = Enchantment.class.getDeclaredField("acceptingNew");
            f.setAccessible(true);
            f.set(null, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        glow = new EnchantmentGlow();
        Enchantment.registerEnchantment(glow);
        return glow;
    }

    public static void addGlow(ItemStack item) {
        ItemMeta hideFlags = item.getItemMeta();
        hideFlags.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(hideFlags);
        item.addUnsafeEnchantment(WATER_WORKER, 0);
    }

    @Override
    public boolean canEnchantItem(ItemStack item) {
        return true;
    }

    @Override
    public boolean conflictsWith(Enchantment other) {
        return false;
    }

    @Override
    public EnchantmentTarget getItemTarget() {
        return null;
    }

    @Override
    public int getMaxLevel() {
        return 10;
    }

    @Override
    public int getStartLevel() {
        return 1;
    }
}