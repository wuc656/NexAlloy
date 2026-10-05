package io.github.nexalloy.morphe.facebook.settings

import android.app.Activity
import android.content.Context
import android.view.View
import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
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

        // 2. Hook View.setContentDescription to reliably intercept the Facebook Logo view creation
        runCatching {
            XposedBridge.hookMethod(
                View::class.java.getMethod("setContentDescription", CharSequence::class.java),
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val desc = param.args.firstOrNull()?.toString() ?: return
                        if (desc.equals("facebook 標誌", ignoreCase = true) || desc.equals("facebook logo", ignoreCase = true)) {
                            val logoView = param.thisObject as? View ?: return
                            attachLogoLongClickListener(logoView)
                        }
                    }
                }
            )
        }
    }

    private fun attachLogoLongClickListener(logoView: View) {
        logoView.isLongClickable = true
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
