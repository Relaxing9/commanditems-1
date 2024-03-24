package me.relaxing9.commanditems.data

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Created by Yamakaja on 26.05.18.
 */
class CommandItemsConfig {
    @JsonProperty("items")
    val items: Map<String, ItemDefinition>? = null
}