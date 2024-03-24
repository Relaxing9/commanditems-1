package me.relaxing9.commanditems.data.action

import com.fasterxml.jackson.annotation.JsonProperty
import me.relaxing9.commanditems.data.ItemDefinition.ExecutionTrace
import me.relaxing9.commanditems.interpreter.InterpretationContext
import org.bukkit.Bukkit
import org.bukkit.permissions.PermissionAttachment

/**
 * Created by Yamakaja on 26.05.18.
 */
class ActionCommand : Action(ActionType.COMMAND) {
    @JsonProperty(value = "by")
    private val commandMode = CommandMode.PLAYER

    @JsonProperty(required = true)
    private val command: String? = null

    @JsonProperty(value = "perm")
    private val providedPermission = "*"
    override fun trace(trace: MutableList<ExecutionTrace>, depth: Int) {
        val line: String
        when (commandMode) {
            CommandMode.PLAYER -> line = String.format("PLAYER: /%s", command)
            CommandMode.CONSOLE -> line = String.format("CONSOLE: %s", command)
            CommandMode.PLAYER_PRIVILEGED -> line = String.format(
                "PLAYER (with added permission %s): /%s",
                providedPermission, command
            )

            else -> throw IllegalStateException("Unexpected trace value: " + commandMode)
        }
        trace.add(ExecutionTrace(depth, line))
    }

    override fun process(context: InterpretationContext) {
        val command = context.resolveLocalsInString(command)
        when (commandMode) {
            CommandMode.PLAYER -> Bukkit.getServer().dispatchCommand(context.player, command)
            CommandMode.CONSOLE -> Bukkit.getServer().dispatchCommand(Bukkit.getServer().consoleSender, command)
            CommandMode.PLAYER_PRIVILEGED -> {
                var attachment: PermissionAttachment? = null
                try {
                    context.player.addAttachment(context.plugin).also { attachment = it }
                        .setPermission(providedPermission, true)
                    Bukkit.getServer().dispatchCommand(context.player, command)
                } finally {
                    if (attachment != null) context.player.removeAttachment(attachment!!)
                }
            }

            else -> throw IllegalStateException("Unexpected process value: " + commandMode)
        }
    }

    enum class CommandMode {
        PLAYER, CONSOLE, PLAYER_PRIVILEGED
    }
}