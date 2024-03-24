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

class ItemStackDeserializer protected constructor() : StdDeserializer<ItemStack?>(ItemStack::class.java) {
    var material: Material? = null
    var name: String? = null
    var lore: MutableList<String>? = null
    var glow = false
    var damage = 0
    var unbreakable = false
    var customModelData: Int? = null

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
        if (lore != null && !lore!!.isEmpty()) {
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
            val fieldName = p.currentName
            if (fieldName == "type") {
                try {
                    material = Material.valueOf(p.nextTextValue())
                } catch (e: IllegalArgumentException) {
                    CommandItems.logger.log(Level.WARNING, "Invalid material type!", e)
                }
            } else if (fieldName == "name") {
                name = p.nextTextValue()
            } else if (fieldName == "lore") {
                lore = Arrays.asList(*p.readValueAs<Array<String>>(Array<String>::class.java))
            } else if (fieldName == "glow") {
                glow = p.nextBooleanValue()
            } else if (fieldName == "damage") {
                damage = p.nextIntValue(0)
            } else if (fieldName == "unbreakable") {
                unbreakable = p.nextBooleanValue()
            } else if (fieldName == "customModelData") {
                customModelData = p.nextIntValue(0)
            }
        }
    }
}