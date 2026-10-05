package io.github.nexalloy.morphe.facebook.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch
import android.app.Activity
import android.app.Service

val DisableAudienceNetwork = patch(
    name = "Disable Audience Network",
    description = "Stops Facebook serving ads to other apps.",
) {
    val components = listOf(
        "com.facebook.ads.internal.ipc.AudienceNetworkRemoteService",
        "com.facebook.ads.internal.ipc.AudienceNetworkRemoteActivity",
        "com.facebook.ads.internal.ipc.AudienceNetworkExportedActivity",
        "com.facebook.ads.AudienceNetworkActivity",
        "com.facebook.audiencenetwork.AudienceNetworkService",
    )
    
    components.forEach { className ->
        val clazz = runCatching { classLoader.loadClass(className) }.getOrNull() ?: return@forEach
        
        runCatching {
            val method = clazz.declaredMethods.firstOrNull { it.name == "onCreate" } ?: return@forEach
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    Logger.printDebug { "Disable Audience Network: Blocked $className" }
                    val obj = param.thisObject
                    if (obj is Activity) {
                        obj.finish()
                    } else if (obj is Service) {
                        obj.stopSelf()
                    }
                    param.result = null
                }
            })
        }
    }
}
