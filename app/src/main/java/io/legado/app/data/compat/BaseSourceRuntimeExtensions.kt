package io.legado.app.data.compat

import android.webkit.JavascriptInterface
import com.script.ScriptBindings
import com.script.buildScriptBindings
import com.script.rhino.RhinoScriptEngine
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppLog
import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.RowUi
import io.legado.app.help.CacheManager
import io.legado.app.help.ConcurrentRateLimiter.Companion.updateConcurrentRate
import io.legado.app.help.JsExtensions
import io.legado.app.help.crypto.SymmetricCryptoAndroid
import io.legado.app.help.http.CookieStore
import io.legado.app.help.source.clearExploreKindsCache
import io.legado.app.help.source.getShareScope
import io.legado.app.model.SharedJsScope.remove
import io.legado.app.utils.GSON
import io.legado.app.utils.GSONStrict
import io.legado.app.utils.fromJsonArray
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.has
import io.legado.app.utils.isMainThread
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language

class LegacySourceScriptFacade(private val source: BaseSource) : JsExtensions {
    override fun getSource(): BaseSource = source
    override fun getTag(): String = source.getTag()

    fun getKey(): String = source.getKey()
    fun getLoginJs(): String? = source.getLoginJs()
    fun login() = source.login()
    fun getHeaderMap(userAgent: String, hasLoginHeader: Boolean = false) =
        source.getHeaderMap(userAgent, hasLoginHeader)
    fun getLoginHeader(): String? = source.getLoginHeader()
    fun getLoginHeaderMap(): Map<String, String>? = source.getLoginHeaderMap()
    fun putLoginHeader(header: String) = source.putLoginHeader(header)
    fun removeLoginHeader() = source.removeLoginHeader()
    fun getLoginInfo(): String? = source.getLoginInfo()
    fun getLoginInfoMap(): MutableMap<String, String> = source.getLoginInfoMap()
    fun putLoginInfo(info: String): Boolean = source.putLoginInfo(info)
    fun removeLoginInfo() = source.removeLoginInfo()
    fun setVariable(variable: String?) = source.setVariable(variable)
    fun putVariable(variable: String?) = source.putVariable(variable)
    fun getVariable(): String = source.getVariable()
    fun put(key: String, value: String): String = source.put(key, value)
    fun get(key: String): String = source.get(key)
    fun refreshExplore() = source.refreshExplore()
    fun refreshJSLib() = source.refreshJSLib()
    fun putConcurrent(value: String) = source.putConcurrent(value)
    fun evalJS(js: String): Any? = source.evalJS(js)
}

private fun BaseSource.jsExtensions(): JsExtensions = LegacySourceScriptFacade(this)

fun BaseSource.getLoginJs(): String? = loginUrl?.let { value ->
    when {
        value.startsWith("@js:") -> value.substring(4)
        value.startsWith("<js>") -> value.substring(4, value.lastIndexOf("<"))
        else -> value
    }
}

@JavascriptInterface
fun BaseSource.login() {
    val loginScript = getLoginJs()
    if (!loginScript.isNullOrBlank()) {
        @Language("js")
        val script = """$loginScript
            if(typeof login=='function'){
                login.apply(this);
            } else {
                throw('Function login not implements!!!')
            }
        """.trimIndent()
        evalJS(script)
    }
}

fun BaseSource.getHeaderMap(
    userAgent: String,
    hasLoginHeader: Boolean = false,
): HashMap<String, String> = HashMap<String, String>().apply {
    header?.let { headerRule ->
        try {
            val json = when {
                headerRule.startsWith("@js:", true) -> evalJS(headerRule.substring(4)).toString()
                headerRule.startsWith("<js>", true) ->
                    evalJS(headerRule.substring(4, headerRule.lastIndexOf("<"))).toString()
                else -> headerRule
            }
            GSONStrict.fromJsonObject<Map<String, String>>(json).getOrNull()?.let(::putAll)
                ?: GSON.fromJsonObject<Map<String, String>>(json).getOrNull()?.let {
                    jsExtensions().log("请求头规则 JSON 格式不规范，请改为规范格式")
                    putAll(it)
                }
        } catch (error: Exception) {
            AppLog.put("执行请求头规则出错\n$error", error)
        }
    }
    if (!has(AppConst.UA_NAME, true)) put(AppConst.UA_NAME, userAgent)
    if (hasLoginHeader) getLoginHeaderMap()?.let(::putAll)
}

@JavascriptInterface
fun BaseSource.getLoginHeader(): String? = CacheManager.get("loginHeader_${getKey()}")

fun BaseSource.getLoginHeaderMap(): Map<String, String>? =
    getLoginHeader()?.let { GSON.fromJsonObject<Map<String, String>>(it).getOrNull() }

fun BaseSource.putLoginHeader(header: String) {
    val map = GSON.fromJsonObject<Map<String, String>>(header).getOrNull()
    (map?.get("Cookie") ?: map?.get("cookie"))?.let { CookieStore.replaceCookie(getKey(), it) }
    CacheManager.put("loginHeader_${getKey()}", header)
}

fun BaseSource.removeLoginHeader() {
    CacheManager.delete("loginHeader_${getKey()}")
    CookieStore.removeCookie(getKey())
}

@JavascriptInterface
fun BaseSource.getLoginInfo(): String? = try {
    val key = AppConst.androidId.encodeToByteArray(0, 16)
    CacheManager.get("userInfo_${getKey()}")?.let {
        SymmetricCryptoAndroid("AES", key).decryptStr(it)
    }
} catch (error: Exception) {
    AppLog.put("获取登陆信息出错", error)
    null
}

private fun loginBindings(): ScriptBindings.() -> Unit = {
    put("result", mutableMapOf<String, String>())
    put("book", null)
    put("chapter", null)
}

fun BaseSource.getLoginInfoMap(): MutableMap<String, String> {
    val stored = getLoginInfo()
    if (stored != null) {
        return GSON.fromJsonObject<MutableMap<String, String>>(stored).getOrNull() ?: mutableMapOf()
    }
    if (loginUi.isNullOrBlank()) return mutableMapOf()
    val json = loginUi?.let { value ->
        when {
            value.startsWith("@js:") -> evalJS(
                "${getLoginJs().orEmpty()}\n${value.substring(4)}",
                loginBindings(),
            ).toString()
            value.startsWith("<js>") -> evalJS(
                "${getLoginJs().orEmpty()}\n${value.substring(4, value.lastIndexOf("<"))}",
                loginBindings(),
            ).toString()
            else -> value
        }
    }
    val result = GSON.fromJsonArray<RowUi>(json).getOrNull()
        ?.filter { it.type != "button" }
        ?.associate { it.name to (it.default ?: "") }
        ?.takeIf { it.isNotEmpty() }
        ?.also { putLoginInfo(GSON.toJson(it)) }
    return result?.toMutableMap() ?: mutableMapOf()
}

@JavascriptInterface
fun BaseSource.putLoginInfo(info: String): Boolean = try {
    val key = AppConst.androidId.encodeToByteArray(0, 16)
    CacheManager.put("userInfo_${getKey()}", SymmetricCryptoAndroid("AES", key).encryptBase64(info))
    true
} catch (error: Exception) {
    AppLog.put("保存登陆信息出错", error)
    false
}

@JavascriptInterface
fun BaseSource.removeLoginInfo() = CacheManager.delete("userInfo_${getKey()}")

fun BaseSource.setVariable(variable: String?) = putVariable(variable)

@JavascriptInterface
fun BaseSource.putVariable(variable: String?) {
    if (variable == null) CacheManager.delete("sourceVariable_${getKey()}")
    else CacheManager.put("sourceVariable_${getKey()}", variable)
}

@JavascriptInterface
fun BaseSource.getVariable(): String =
    getTemporaryVariable() ?: CacheManager.get("sourceVariable_${getKey()}") ?: ""

@JavascriptInterface
fun BaseSource.put(key: String, value: String): String {
    CacheManager.put("v_${getKey()}_$key", value)
    return value
}

@JavascriptInterface
fun BaseSource.get(key: String): String = CacheManager.get("v_${getKey()}_$key") ?: ""

fun BaseSource.refreshExplore() {
    check(!isMainThread) { "refreshExplore must be called on a background thread" }
    runBlocking { if (this@refreshExplore is BookSource) clearExploreKindsCache() }
}

fun BaseSource.refreshJSLib() {
    check(!isMainThread) { "refreshJSLib must be called on a background thread" }
    runBlocking { remove(jsLib) }
}

fun BaseSource.putConcurrent(value: String) = updateConcurrentRate(getKey(), value)

@Throws(Exception::class)
fun BaseSource.evalJS(
    jsStr: String,
    bindingsConfig: ScriptBindings.() -> Unit = {},
): Any? {
    val bindings = buildScriptBindings { values ->
        values["java"] = this
        values["source"] = this
        values["baseUrl"] = getKey()
        values["cookie"] = CookieStore
        values["cache"] = CacheManager
        values.apply(bindingsConfig)
    }
    val sharedScope = getShareScope()
    val scope = if (sharedScope == null) {
        RhinoScriptEngine.getRuntimeScope(bindings)
    } else {
        bindings.apply { prototype = sharedScope }
    }
    return RhinoScriptEngine.eval(jsStr, scope)
}
