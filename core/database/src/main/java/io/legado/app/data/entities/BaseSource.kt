package io.legado.app.data.entities

/**
 * Persisted source identity and configuration contract.
 * Network, JavaScript, cookies, encryption and cache behavior live in the app compatibility layer.
 */
interface BaseSource {
    var concurrentRate: String?
    var loginUrl: String?
    var loginUi: String?
    var header: String?
    var enabledCookieJar: Boolean?
    var jsLib: String?

    fun getTag(): String
    fun getKey(): String
    fun getSource(): BaseSource = this
    fun setTemporaryVariable(variable: String?) = Unit
    fun getTemporaryVariable(): String? = null
}
