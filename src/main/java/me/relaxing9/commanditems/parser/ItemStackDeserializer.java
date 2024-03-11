package me.relaxing9.commanditems.parser;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.base.Preconditions;

import me.relaxing9.commanditems.CommandItems;

public class ItemStackDeserializer extends StdDeserializer<ItemStack> {

    public Material material = null;
    public String name = null;
    public List<String> lore = null;
    public boolean glow = false;
    public int damage = 0;
    public boolean unbreakable = false;
    public Integer customModelData = null;

    protected ItemStackDeserializer() {
        super(ItemStack.class);
    }

    @Override
    public ItemStack deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        
        getToken(p);

        Preconditions.checkNotNull(material, "No material specified!");

        ItemStack stack = new ItemStack(material, 1);

        Damageable newDamage = (Damageable) stack.getItemMeta();
        newDamage.setDamage(damage);
        stack.setItemMeta((ItemMeta) newDamage);

        ItemMeta meta = stack.getItemMeta();

        Preconditions.checkNotNull(meta, "ItemMeta is null! (Material: " + material + ")");

        if (name != null)
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

        if (lore != null && !lore.isEmpty()) {
            for (int i = 0; i < lore.size(); i++) {
                lore.set(i, ChatColor.translateAlternateColorCodes('&', lore.get(i)));
            }
            meta.setLore(lore);
        }
        
        meta.setUnbreakable(unbreakable);
        
        if (customModelData != null) {
            meta.setCustomModelData(customModelData);
        }

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        stack.setItemMeta(meta);

        if (glow)
            stack.addUnsafeEnchantment(Enchantment.WATER_WORKER, 0);

        return stack;
    }

    public void getToken (JsonParser p) throws IOException, JsonProcessingException {
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.getCurrentName();
            if (fieldName.equals("type")) {
                try {
                    material = Material.valueOf(p.nextTextValue());
                } catch (IllegalArgumentException e) {
                    CommandItems.logger.log(Level.WARNING, "Invalid material type!", e);
                }
            } else if (fieldName.equals("name")) {
                name = p.nextTextValue();
            } else if (fieldName.equals("lore")) {
                lore = Arrays.asList(p.readValueAs(String[].class));
            } else if (fieldName.equals("glow")) {
                glow = p.nextBooleanValue();
            } else if (fieldName.equals("damage")) {
                damage = p.nextIntValue(0);
            } else if (fieldName.equals("unbreakable")) {
                unbreakable = p.nextBooleanValue();
            } else if (fieldName.equals("customModelData")) {
                customModelData = p.nextIntValue(0);
            }
        }
    }
}
