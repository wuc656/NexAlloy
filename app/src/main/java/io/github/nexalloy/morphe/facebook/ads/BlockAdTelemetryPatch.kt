package io.github.nexalloy.morphe.facebook.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch

val BlockAdTelemetry = patch(
    name = "Block ad telemetry",
    description = "Stops Facebook watching for screenshots of ads and reporting which apps you install for ad attribution.",
) {
    val telemetryClasses = listOf(
        "com.facebook.ads.screenshot.AdsScreenshotController",
        "com.facebook.ads.AdsScreenshotDetector",
        "com.facebook.feed.platformads.AppInstallTrackerScheduler",
        "com.facebook.feed.platformads.AppInstallService"
    )

    telemetryClasses.forEach { className ->
        val clazz = runCatching { classLoader.loadClass(className) }.getOrNull() ?: return@forEach
        
        clazz.declaredMethods.filter { it.returnType == Void.TYPE }.forEach { method ->
            runCatching {
                XposedBridge.hookMethod(method, object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        if (!io.github.nexalloy.morphe.facebook.settings.FacebookSettings.isEnabled(
                                io.github.nexalloy.morphe.facebook.settings.FacebookSettings.KEY_BLOCK_AD_TELEMETRY, true
                            )) {
                            return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                        }
                        Logger.printDebug { "Block ad telemetry: Blocked ${className}.${method.name}" }
                        return null
                    }
                })
            }
        }
    }
}
