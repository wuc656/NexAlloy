package io.github.nexalloy.morphe.facebook.navigation

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch

private const val OVERRIDE_KEY = "fb4a_bottom_tabs_override_enabled"
private const val TRI_STATE_CLASS = "com.facebook.common.util.TriState"

/**
 * Fingerprint matching the override key definition class or preferences reader
 */
internal object BottomTabsOverrideKeyFingerprint : Fingerprint(
    strings = listOf(OVERRIDE_KEY)
)

/**
 * Tab bar at the bottom:
 * Moves Facebook's tab bar to the bottom of the screen on accounts that have it at the top.
 */
val TabBarAtTheBottom = patch(
    name = "Tab bar at the bottom",
    description = "Moves Facebook's tab bar to the bottom of the screen on accounts that have it at the top.",
) {
    // When enabled, force TriState.YES for bottom tab bar override
    val triStateClass = runCatching { classLoader.loadClass(TRI_STATE_CLASS) }.getOrNull()
    val yesConstant = triStateClass?.getField("YES")?.get(null)

    if (yesConstant != null) {
        BottomTabsOverrideKeyFingerprint.hookMethod(object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (!FacebookSettings.isEnabled(FacebookSettings.KEY_BOTTOM_TAB_BAR, false)) return

                if (param.result != null && param.result?.javaClass == triStateClass) {
                    Logger.printDebug { "TabBarAtTheBottom: Override tab bar position to YES" }
                    param.result = yesConstant
                }
            }
        })
    }
}
