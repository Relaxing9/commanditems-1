package me.relaxing9.commanditems.data.action

import com.fasterxml.jackson.annotation.JsonProperty
import me.relaxing9.commanditems.CommandItems
import me.relaxing9.commanditems.data.ItemDefinition.ExecutionTrace
import me.relaxing9.commanditems.interpreter.InterpretationContext
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.util.logging.Level

class ActionMessage(
    @JsonProperty("action") type: ActionType?,
    @param:JsonProperty("to") private val target: MessageTarget?,
    @JsonProperty(value = "message", required = true) message: String?,
    @JsonProperty("perm") permission: String?
) :
    Action(type!!) {
    private val message: String
    private val permission: String?

    init {
        if (target == null) target = MessageTarget.PLAYER
        this.message = ChatColor.translateAlternateColorCodes('&', message!!)
        this.permission = permission
    }

    override fun trace(trace: MutableList<ExecutionTrace>, depth: Int) {
        val line: String
        when (target) {
            MessageTarget.PLAYER -> line = String.format("To player: %s", message)
            MessageTarget.CONSOLE -> line = String.format("To console: %s", message)
            MessageTarget.EVERYBODY -> line = String.format("To everybody: %s", message)
            MessageTarget.PERMISSION -> line = String.format(
                "To everybody with permission %s: %s",
                permission, message
            )

            else -> throw IllegalStateException("Unexpected trace value: $target")
        }
        trace.add(ExecutionTrace(depth, line))
    }

    override fun process(context: InterpretationContext) {
        val message = context.resolveLocalsInString(message)
        when (target) {
            MessageTarget.PLAYER -> context.player.sendMessage(message)
            MessageTarget.CONSOLE -> context.plugin.logger.info("[MSG] $message")
            MessageTarget.EVERYBODY -> Bukkit.getServer().broadcastMessage(message)
            MessageTarget.PERMISSION -> {
                if (permission == null) CommandItems.logger.log(
                    Level.SEVERE,
                    "[CMDI] Permission is null in permission node!"
                )
                Bukkit.getOnlinePlayers().stream()
                    .filter { player: Player? -> player!!.hasPermission(permission!!) }
                    .forEach { player: Player? -> player!!.sendMessage(message) }
            }

            else -> throw IllegalStateException("Unexpected process value: $target")
        }
    }

    /**
     * Created by Yamakaja on 26.05.18.
     */
    enum class MessageTarget {
        PLAYER, CONSOLE, EVERYBODY, PERMISSION
    }
}