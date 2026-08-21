package io.legado.app.data.entities

import io.legado.app.data.runtime.EntityVariableRuntime
import io.legado.app.model.analyzeRule.RuleDataInterface
import io.legado.app.utils.GSON

interface BaseRssArticle : RuleDataInterface {

    var origin: String
    var link: String

    var variable: String?

    override fun putVariable(key: String, value: String?): Boolean {
        if (super.putVariable(key, value)) {
            variable = GSON.toJson(variableMap)
        }
        return true
    }

    override fun putBigVariable(key: String, value: String?) {
        EntityVariableRuntime.store.putRss(origin, link, key, value)
    }

    override fun getBigVariable(key: String): String? {
        return EntityVariableRuntime.store.getRss(origin, link, key)
    }

}
