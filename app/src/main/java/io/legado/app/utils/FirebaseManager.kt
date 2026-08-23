package io.legado.app.utils

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import io.legado.app.help.config.AppConfig

object FirebaseManager {

    val isEnabled: Boolean
        get() = AppConfig.firebaseEnable

    fun init(context: Context) {
        applyState(context, isEnabled)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        applyState(context, enabled)
    }

    private fun applyState(context: Context, enabled: Boolean) {
        if (enabled) {
            if (FirebaseApp.getApps(context).isEmpty()) {
                // 没有 google-services 配置时返回 null（benchmark 变体即如此），
                // 此时不能再调 FirebaseAnalytics.getInstance，会抛“Default FirebaseApp is not initialized”。
                if (FirebaseApp.initializeApp(context) == null) return
            }
            FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(true)
        } else {
            try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(false)
                    FirebaseApp.getInstance().delete()
                }
            } catch (_: Exception) {
                // 忽略异常
            }
        }
    }
}
