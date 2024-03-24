package me.relaxing9.commanditems.interpreter

/**
 * Created by Yamakaja on 26.05.18.
 */
class InterpretationStackFrame {
    private val locals: MutableMap<String, String> = HashMap()
    fun getLocal(key: String): String? {
        return locals[key]
    }

    fun pushLocal(key: String, value: String) {
        locals.put(key, value)
    }

    fun reset() {
        locals.clear()
    }

    fun getLocals(): Map<String, String> {
        return locals
    }

    fun copy(into: InterpretationStackFrame): InterpretationStackFrame {
        into.locals.putAll(locals)
        return into
    }
}