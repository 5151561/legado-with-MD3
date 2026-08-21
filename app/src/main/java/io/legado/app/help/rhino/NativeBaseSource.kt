package io.legado.app.help.rhino

import com.script.rhino.JavaObjectWrapFactory
import io.legado.app.data.compat.LegacySourceScriptFacade
import io.legado.app.data.entities.BaseSource
import org.mozilla.javascript.NativeJavaObject
import org.mozilla.javascript.Scriptable

class NativeBaseSource(scope: Scriptable?, javaObject: Any, staticType: Class<*>?) :
    NativeJavaObject(scope, javaObject, staticType) {

    private val compatibilityObject = NativeJavaObject(
        scope,
        LegacySourceScriptFacade(javaObject as BaseSource),
        LegacySourceScriptFacade::class.java,
    )

    override fun has(name: String, start: Scriptable): Boolean {
        if (name != "setVariable" && name.length > 3 && name.startsWith("set")) {
            val name = name.substring(3).replaceFirstChar { it.lowercase() }
            if (super.has(name, start)) {
                return false
            }
        }
        return super.has(name, start) || compatibilityObject.has(name, compatibilityObject)
    }

    override fun get(name: String, start: Scriptable): Any? {
        if (name != "setVariable" && name.length > 3 && name.startsWith("set")) {
            val name = name.substring(3).replaceFirstChar { it.lowercase() }
            if (super.has(name, start)) {
                return NOT_FOUND
            }
        }
        val value = super.get(name, start)
        return if (value === NOT_FOUND) {
            compatibilityObject.get(name, compatibilityObject)
        } else {
            value
        }
    }

    override fun put(
        name: String,
        start: Scriptable,
        value: Any?
    ) {
        if (name == "variable") {
            super.put(name, start, value)
        }
    }

    companion object {
        val factory = JavaObjectWrapFactory { scope, javaObject, staticType ->
            NativeBaseSource(scope, javaObject, staticType)
        }
    }

}
