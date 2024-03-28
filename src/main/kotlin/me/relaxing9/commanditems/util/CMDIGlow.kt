package me.relaxing9.commanditems.util

import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.enchantments.EnchantmentWrapper
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import java.lang.reflect.Field

class CMDIGlow : EnchantmentWrapper("enchantment_glow") {
    override fun canEnchantItem(item: ItemStack): Boolean {
        return true
    }

    override fun conflictsWith(other: Enchantment): Boolean {
        return false
    }

    override fun getItemTarget(): EnchantmentTarget {
        return null
    }

    override fun getMaxLevel(): Int {
        return 10
    }

    override fun getStartLevel(): Int {
        return 1
    }

    companion object {
        var glow: Enchantment? = null
            get() {
                if (field != null) return field else if (WATER_WORKER.also { field = it } != null) return field
                try {
                    val f: Field = Enchantment::class.java.getDeclaredField("acceptingNew")
                    f.isAccessible = true
                    f[null] = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                field = CMDIGlow()
                registerEnchantment(field as CMDIGlow)
                return field
            }
            private set

        fun addGlow(item: ItemStack) {
            val hideFlags = item.itemMeta
            hideFlags!!.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            item.itemMeta = hideFlags
            item.addUnsafeEnchantment(WATER_WORKER, 0)
        }
    }
}