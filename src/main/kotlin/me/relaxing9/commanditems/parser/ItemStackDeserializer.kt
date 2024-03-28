package me.relaxing9.commanditems.parser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.google.common.base.Preconditions
import me.relaxing9.commanditems.CommandItems
import me.relaxing9.commanditems.util.CMDIGlow
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import java.io.IOException
import java.util.*
import java.util.logging.Level

open class ItemStackDeserializer internal constructor() : StdDeserializer<ItemStack?>(ItemStack::class.java) {
    private var material: Material? = null
    var name: String? = null
    private var lore: MutableList<String>? = null
    private var glow = false
    private var damage = 0
    private var unbreakable = false
    private var customModelData: Int? = null

    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): ItemStack {
        getToken(p)
        Preconditions.checkNotNull(material, "No material specified!")
        val stack = ItemStack(material!!, 1)
        val newDamage = stack.itemMeta as Damageable?
        newDamage!!.damage = damage
        stack.itemMeta = newDamage as ItemMeta?
        val meta = stack.itemMeta
        Preconditions.checkNotNull(meta, "ItemMeta is null! (Material: $material)")
        if (name != null) meta!!.setDisplayName(ChatColor.translateAlternateColorCodes('&', name!!))
        if (lore != null && lore!!.isNotEmpty()) {
            for (i in lore.indices) {
                lore!![i] = ChatColor.translateAlternateColorCodes('&', lore!![i])
            }
            meta!!.lore = lore
        }
        meta!!.isUnbreakable = unbreakable
        if (customModelData != null) {
            meta.setCustomModelData(customModelData)
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        stack.itemMeta = meta
        if (glow) stack.addUnsafeEnchantment(CMDIGlow.getGlow(), 0)
        return stack
    }

    @Throws(IOException::class, JsonProcessingException::class)
    fun getToken(p: JsonParser) {
        while (p.nextToken() != JsonToken.END_OBJECT) {
            when (p.currentName) {
                "type" -> {
                    try {
                        material = Material.valueOf(p.nextTextValue())
                    } catch (e: IllegalArgumentException) {
                        CommandItems.logger.log(Level.WARNING, "Invalid material type!", e)
                    }
                }
                "name" -> {
                    name = p.nextTextValue()
                }
                "lore" -> {
                    lore = listOf(*p.readValueAs(Array<String>::class.java))
                }
                "glow" -> {
                    glow = p.nextBooleanValue()
                }
                "damage" -> {
                    damage = p.nextIntValue(0)
                }
                "unbreakable" -> {
                    unbreakable = p.nextBooleanValue()
                }
                "customModelData" -> {
                    customModelData = p.nextIntValue(0)
                }
            }
        }
    }
}