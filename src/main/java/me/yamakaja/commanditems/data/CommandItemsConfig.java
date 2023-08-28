package me.yamakaja.commanditems.data;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Yamakaja on 26.05.18.
 */
public class CommandItemsConfig {

    @JsonProperty("items")
    private Map<String, ItemDefinition> items;

    public Map<String, ItemDefinition> getItems() {
        return this.items;
    }

}
