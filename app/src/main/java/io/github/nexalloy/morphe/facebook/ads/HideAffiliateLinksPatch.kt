package io.github.nexalloy.morphe.facebook.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch
import java.lang.reflect.Modifier

private const val AFFILIATE_PLUGIN =
    "com.facebook.feedback.comments.plugins.indicatorpill.organicaffiliatefloatingcta.OrganicAffiliateFloatingCtaPlugin"

val HideAffiliateLinks = patch(
    name = "Hide affiliate product links",
    description = "Removes the product cards of affiliate shop links from reels, feed posts and the comment sheet.",
) {
    // The plugin keeps its name; its static one-argument readers return the card model.
    // Hooked by reflection: a DexKit fingerprint on the class alone matches many methods.
    val plugin = runCatching { classLoader.loadClass(AFFILIATE_PLUGIN) }.getOrNull() ?: run {
        Logger.printInfo { "Hide affiliate links: $AFFILIATE_PLUGIN not in this build" }
        return@patch
    }
    val readers = plugin.declaredMethods.filter {
        Modifier.isStatic(it.modifiers) && it.parameterTypes.size == 1 && !it.returnType.isPrimitive
    }
    readers.forEach { method ->
        XposedBridge.hookMethod(method, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (!FacebookSettings.isEnabled(FacebookSettings.KEY_HIDE_AFFILIATE_LINKS, true)) return
                Logger.printDebug { "Hide affiliate links: Blocked floating card" }
                param.result = null
            }
        })
    }
    Logger.printInfo { "Hide affiliate links: hooked ${readers.size} readers" }
}
