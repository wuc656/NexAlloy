package io.github.nexalloy.morphe.facebook.feed

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch

private const val FEED_FILTERS_FRAGMENT_CLASS = "com.facebook.feed.fragment.FeedFiltersFragment"
private const val NAV_BAR_METHOD = "shouldInitializeNavBar"

/**
 * Hide the Feeds header:
 * Takes the title row and filter pills off the top of the Feeds tab.
 */
val HideFeedsHeader = patch(
    name = "Hide the Feeds header",
    description = "Takes the title row and filter pills off the top of the Feeds tab.",
) {
    val fragmentClass = runCatching { classLoader.loadClass(FEED_FILTERS_FRAGMENT_CLASS) }.getOrNull()
    if (fragmentClass == null) {
        Logger.printDebug { "HideFeedsHeader: FeedFiltersFragment class not found" }
        return@patch
    }

    val navBarMethod = fragmentClass.declaredMethods.firstOrNull { it.name == NAV_BAR_METHOD }
    if (navBarMethod != null) {
        XposedBridge.hookMethod(navBarMethod, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (FacebookSettings.isEnabled(FacebookSettings.KEY_HIDE_FEEDS_HEADER, false)) {
                    param.result = false
                }
            }
        })
    }
}
