package me.relaxing9.commanditems.interpreter

import me.relaxing9.commanditems.CommandItems
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.function.BiConsumer
import java.util.logging.Level

/**
 * Created by Yamakaja on 26.05.18.
 */
class InterpretationContext {
    private val interpretationStack: Deque<InterpretationStackFrame> = ArrayDeque()
    var plugin: CommandItems
        private set
    var player: Player
        private set

    constructor(plugin: CommandItems, player: Player) {
        this.plugin = plugin
        this.player = player
    }

    constructor(context: InterpretationContext) {
        plugin = context.plugin
        player = context.player
        for (frame in context.interpretationStack) interpretationStack.add(
            frame.copy(
                newFrame
            )
        )
    }

    private val newFrame: InterpretationStackFrame
        private get() = if (stackFrameCache.size < 1) InterpretationStackFrame() else stackFrameCache.remove()

    fun pushFrame() {
        interpretationStack.addFirst(InterpretationStackFrame())
    }

    fun popFrame() {
        val stackFrame = interpretationStack.removeFirst()
        stackFrame.reset()
        stackFrameCache.push(stackFrame)
    }

    fun pushLocal(key: String?, value: String?) {
        interpretationStack.first.pushLocal(key, value)
    }

    private fun resolveLocal(key: String?): String? {
        val iterator: Iterator<InterpretationStackFrame> = interpretationStack.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            val result = next.getLocal(key)
            if (result != null) return result
        }
        return null
    }

    fun forEachNumericLocal(consumer: BiConsumer<String?, Double?>) {
        val iterator = interpretationStack.descendingIterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            for ((key, value) in next.getLocals()) {
                try {
                    consumer.accept(key, value.toDouble())
                } catch (ignored: Exception) {
                }
            }
        }
    }

    fun resolveLocalsInString(input: String): String {
        val chars: CharArray = input.toCharArray()
        val outputBuilder = StringBuilder()
        var escaped = false
        var i = 0
        while (i < chars.size) {
            if (escaped) {
                outputBuilder.append(chars[i])
                escaped = false
                i++
                continue
            } else if (chars[i] == '\\') {
                escaped = true
                i++
                continue
            }
            if (chars[i] != '{') {
                outputBuilder.append(chars[i])
                i++
                continue
            }
            val end: Int = input.indexOf('}', i)
            if (end == -1) CommandItems.logger.log(Level.WARNING, "Unterminated curly braces!")
            val localName: String = input.substring(i + 1, end)
            val local = resolveLocal(localName)
            if (local == null) CommandItems.logger.log(
                Level.SEVERE,
                "Attempt to access undefined local '$localName'!"
            )
            outputBuilder.append(local)
            i = end
            i++
        }
        return outputBuilder.toString()
    }

    fun copy(): InterpretationContext {
        return InterpretationContext(this)
    }

    protected fun finalize() {
        release()
    }

    fun release() {
        for (frame in interpretationStack) {
            frame.reset()
            addToCache(frame)
        }
        interpretationStack.clear()
    }

    companion object {
        private val cacheLock: Lock = ReentrantLock()
        private val stackFrameCache: Deque<InterpretationStackFrame> = ArrayDeque()
        private fun addToCache(frame: InterpretationStackFrame) {
            try {
                cacheLock.lock()
                stackFrameCache.push(frame)
            } finally {
                cacheLock.unlock()
            }
        }
    }
}