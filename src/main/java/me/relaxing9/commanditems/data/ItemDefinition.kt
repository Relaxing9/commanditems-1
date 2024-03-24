package me.relaxing9.commanditems.data

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.base.Preconditions
import de.tr7zw.changeme.nbtapi.NBTItem
import me.relaxing9.commanditems.data.action.Action
import me.relaxing9.commanditems.util.CMDIGlow
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import java.util.*
import java.util.stream.Collectors

class ItemDefinition {
    @kotlin.jvm.Transient
    private var key: String? = null

    @JsonProperty
    val isConsumed = false

    @JsonProperty
    val cooldown: Long = 0

    @JsonProperty
    private val item: ItemStackBuilder? = null

    @JsonProperty
    val actions: Array<Action>

    @JsonProperty
    val isSneaking = false

    @JsonProperty
    val parameters: Map<String, String>? = null

    class ItemStackBuilder {
        @JsonProperty(required = true)
        private val type: Material? = null

        @JsonProperty(required = true)
        private val name: String? = null

        @JsonProperty
        private val lore: List<String>? = null

        @JsonProperty(defaultValue = "false")
        private val glow = false

        @JsonProperty(defaultValue = "0")
        private val damage = 0

        @JsonProperty(defaultValue = "false")
        private val unbreakable = false

        @JsonProperty(required = false)
        private val customModelData: Int? = null

        @JsonProperty(defaultValue = "")
        private val skullUser: String? = null
        fun build(key: String?, params: Map<String?, String?>?): ItemStack {
            Preconditions.checkNotNull(type, "No material specified!")
            val stack = ItemStack(type!!, 1)
            val newDamage = stack.itemMeta as Damageable?
            newDamage!!.damage = damage
            stack.itemMeta = newDamage as ItemMeta?
            val meta = stack.itemMeta
            Preconditions.checkNotNull(meta, "ItemMeta is null! (Material: $type)")
            if (name != null) meta!!.setDisplayName(ChatColor.translateAlternateColorCodes('&', name))
            if (lore != null && !lore.isEmpty()) meta!!.lore = lore.stream()
                .map { x: String? ->
                    ChatColor.translateAlternateColorCodes(
                        '&',
                        x!!
                    )
                }
                .collect(Collectors.toList())
            meta!!.isUnbreakable = unbreakable
            if (customModelData != null) meta.setCustomModelData(customModelData)
            if (type == Material.PLAYER_HEAD && skullUser != null && !skullUser.isEmpty()) {
                val skullMeta = meta as SkullMeta?
                val player = getSkullMeta(skullUser)
                skullMeta!!.owningPlayer = player
            }
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            stack.itemMeta = meta
            val nbti = NBTItem(stack)
            nbti.getOrCreateCompound("cmdi").setString("command", key)
            nbti.getOrCreateCompound("cmdi").setObject("params", params)
            nbti.applyNBT(stack)
            if (glow) stack.addUnsafeEnchantment(CMDIGlow.getGlow(), 0)
            return stack
        }
    }

    class ExecutionTrace(val depth: Int, val label: String)

    fun setKey(key: String?) {
        this.key = key
    }

    fun getItem(params: Map<String?, String?>?): ItemStack {
        return item!!.build(key, params)
    }

    val executionTrace: List<ExecutionTrace?>
        get() {
            val trace: List<ExecutionTrace?> = ArrayList()
            for (action in actions) action.trace(trace, 0)
            return trace
        }

    companion object {
        private fun getSkullMeta(skullUser: String?): OfflinePlayer {
            return try {
                val uuid = UUID.fromString(skullUser)
                Bukkit.getOfflinePlayer(uuid)
            } catch (e: IllegalArgumentException) {
                Bukkit.getOfflinePlayer(skullUser!!)
            }
        }
    }
}