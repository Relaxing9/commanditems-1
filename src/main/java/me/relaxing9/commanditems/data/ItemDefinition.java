package me.relaxing9.commanditems.data;

import com.jeff_media.morepersistentdatatypes.DataType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import me.relaxing9.commanditems.CommandItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

import me.relaxing9.commanditems.data.action.Action;
import me.relaxing9.commanditems.util.CMDIGlow;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ItemDefinition {

    private transient String key;

    @JsonProperty
    private boolean consumed;

    @JsonProperty
    private long cooldown;

    @JsonProperty
    private ItemStackBuilder item;

    @JsonProperty
    private Action[] actions;

    @JsonProperty
    private boolean sneaking;

    @JsonProperty
    private Map<String, String> parameters;

    public static class ItemStackBuilder {
        @JsonProperty(required = true)
        private Material type;

        @JsonProperty(required = true)
        private String name;

        @JsonProperty
        private List<String> lore;

        @JsonProperty(defaultValue = "false")
        private boolean glow;

        @JsonProperty(defaultValue = "0")
        private int damage;

        @JsonProperty(defaultValue = "false")
        private boolean unbreakable;

        @JsonProperty(required = false)
        private Integer customModelData;

        @JsonProperty(defaultValue = "")
        private String skullUser;

        @SuppressWarnings("deprecation")
        public ItemStack build(CommandItems plugin, String key, Map<String, String> params) {
            Preconditions.checkNotNull(this.type, "No material specified!");

            ItemStack stack = new ItemStack(this.type, 1);
            Damageable newDamage = (Damageable) stack.getItemMeta();
            newDamage.setDamage(this.damage);
            stack.setItemMeta((ItemMeta) newDamage);
            ItemMeta meta = stack.getItemMeta();

            Preconditions.checkNotNull(meta, "ItemMeta is null! (Material: " + type + ")");

            if (name != null)
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

            if (lore != null && !this.lore.isEmpty())
                meta.setLore(lore.stream()
                        .map(x -> ChatColor.translateAlternateColorCodes('&', x))
                        .collect(Collectors.toList()));

            meta.setUnbreakable(unbreakable);
            if (customModelData != null)
                meta.setCustomModelData(customModelData);

            if (this.type == Material.PLAYER_HEAD && skullUser != null && !skullUser.isEmpty()) {
                SkullMeta skullMeta = (SkullMeta) meta;
                OfflinePlayer player = getSkullMeta(this.skullUser);

                skullMeta.setOwningPlayer(player);
            }

            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            PersistentDataContainer container = stack.getItemMeta().getPersistentDataContainer();
            container.set(new NamespacedKey(plugin, "command"), PersistentDataType.STRING, key);
            container.set(new NamespacedKey(plugin, "params"), DataType.asHashMap(DataType.STRING, DataType.STRING), new HashMap<>(params));

            stack.setItemMeta(meta);

            if (glow)
                stack.addUnsafeEnchantment(CMDIGlow.getGlow(), 0);

            return stack;
        }

    }

    @SuppressWarnings("deprecation")
    private static OfflinePlayer getSkullMeta(String skullUser) {
        try {
            UUID uuid = UUID.fromString(skullUser);
            return Bukkit.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException e) {
            return Bukkit.getOfflinePlayer(skullUser);
        }
    }

    public static class ExecutionTrace {
        public final int depth;
        public final String label;

        public ExecutionTrace(int depth, String label) {
            this.depth = depth;
            this.label = label;
        }
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Map<String, String> getParameters() {
        return this.parameters;
    }

    public boolean isConsumed() {
        return this.consumed;
    }

    public long getCooldown() {
        return this.cooldown;
    }

    public ItemStack getItem(CommandItems plugin, Map<String, String> params) {
        return this.item.build(plugin, this.key, params);
    }

    public Action[] getActions() {
        return this.actions;
    }

    public boolean isSneaking() {
        return this.sneaking;
    }

    public List<ExecutionTrace> getExecutionTrace() {
        List<ExecutionTrace> trace = new ArrayList<>();

        for (Action action : this.actions) action.trace(trace, 0);

        return trace;
    }

}
