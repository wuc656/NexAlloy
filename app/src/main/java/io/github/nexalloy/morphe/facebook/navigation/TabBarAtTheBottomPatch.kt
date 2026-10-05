package io.github.nexalloy.morphe.facebook.navigation

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch

private const val OVERRIDE_KEY = "fb4a_bottom_tabs_override_enabled"
private const val TRI_STATE_CLASS = "com.facebook.common.util.TriState"
private const val FB_SHARED_PREFERENCES = "com.facebook.prefs.shared.FbSharedPreferences"

/**
 * Tab bar at the bottom:
 * Moves Facebook's tab bar to the bottom of the screen on accounts that have it at the top.
 */
val TabBarAtTheBottom = patch(
    name = "Tab bar at the bottom",
    description = "Moves Facebook's tab bar to the bottom of the screen on accounts that have it at the top.",
) {
    val triStateClass = runCatching { classLoader.loadClass(TRI_STATE_CLASS) }.getOrNull()
    val yesConstant = triStateClass?.getField("YES")?.get(null)
    val fbPrefsInterface = runCatching { classLoader.loadClass(FB_SHARED_PREFERENCES) }.getOrNull()

    if (yesConstant != null && fbPrefsInterface != null) {
        // Hook all methods returning TriState on implementations of FbSharedPreferences
        for (method in fbPrefsInterface.methods) {
            if (method.returnType == triStateClass) {
                runCatching {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (!FacebookSettings.isEnabled(FacebookSettings.KEY_BOTTOM_TAB_BAR, false)) return

                            // Check if any argument's string representation matches the override key
                            val matchesKey = param.args?.any { arg ->
                                arg != null && arg.toString().contains(OVERRIDE_KEY)
                            } ?: false

                            if (matchesKey) {
                                Logger.printDebug { "TabBarAtTheBottom: Override tab bar position to YES" }
                                param.result = yesConstant
                            }
                        }
                    })
                }
            }
        }
    }
}
