package io.github.nexalloy.morphe.facebook.settings

import android.app.Activity
import android.content.Context
import android.view.View
import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.PatchExecutor
import java.lang.ref.WeakReference

object FacebookSettingsHook {
    private var currentActivity: WeakReference<Activity>? = null

    fun initialize(executor: PatchExecutor) {
        val classLoader = executor.classLoader

        // 1. Hook Application / Activity Lifecycle to keep track of current Activity
        runCatching {
            val appClass = classLoader.loadClass("com.facebook.katana.app.FacebookApplication")
            XposedBridge.hookAllMethods(appClass, "onCreate", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val app = param.thisObject as? Context ?: return
                    FacebookSettings.init(app)
                    Logger.printInfo { "Hushfacebook initialized in FacebookApplication" }
                }
            })
        }

        runCatching {
            val activityClass = classLoader.loadClass("com.facebook.katana.activity.FbMainTabActivity")
            XposedBridge.hookAllMethods(activityClass, "onResume", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as? Activity ?: return
                    currentActivity = WeakReference(activity)
                    FacebookSettings.init(activity)
                }
            })
        }

        // 2. Hook WordmarkNavigationBar to register Long Click on the Facebook Logo
        runCatching {
            val navBarClass = classLoader.loadClass("com.facebook.navigation.navbar.legacy.search.WordmarkNavigationBar")
            navBarClass.declaredMethods.forEach { method ->
                // Look for method creating or configuring the wordmark / logo view
                if (View::class.java.isAssignableFrom(method.returnType)) {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val logoView = param.result as? View ?: return
                            attachLogoLongClickListener(logoView)
                        }
                    })
                }
            }
        }
    }

    private fun attachLogoLongClickListener(logoView: View) {
        logoView.setOnLongClickListener { v ->
            val act = currentActivity?.get() ?: (v.context as? Activity)
            if (act != null && !act.isFinishing && !act.isDestroyed) {
                Logger.printInfo { "Opening Hushfacebook Settings from Logo Long-Press" }
                FacebookSettingsDialog.show(act)
                true
            } else {
                false
            }
        }
    }
}
