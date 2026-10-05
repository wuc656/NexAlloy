package io.github.nexalloy.morphe.facebook.stories

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch

internal object StoryPostProcessFingerprint : Fingerprint(
    returnType = "LX/1yk;",
    strings = listOf("StoriesTrayLightFetchControllerQueryOps.postProcessResult")
)

val HideSuggestedStories = patch(
    name = "Hide suggested stories",
    description = "Removes the stories Facebook suggests from people and Pages you don't follow, the ones marked Suggested in the Stories tray.",
) {
    runCatching {
        StoryPostProcessFingerprint.hookMethod(object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (!FacebookSettings.isEnabled(FacebookSettings.KEY_HIDE_SUGGESTED_STORIES, true)) return
                val trayData = param.result ?: return
                // Filter the ImmutableList field inside TrayData (LX/1yk;)
                for (field in trayData.javaClass.declaredFields) {
                    if (List::class.java.isAssignableFrom(field.type)) {
                        field.isAccessible = true
                        val list = field.get(trayData) as? List<*> ?: continue
                        val filtered = list.filter { bucket ->
                            if (bucket == null) return@filter true
                            val isSuggested = bucket.javaClass.declaredMethods.any { method ->
                                method.parameterTypes.isEmpty() &&
                                (method.name.contains("suggested", ignoreCase = true) || method.name.contains("isSuggested", ignoreCase = true)) &&
                                (runCatching { method.invoke(bucket) as? Boolean }.getOrNull() == true)
                            }
                            !isSuggested
                        }
                        if (filtered.size != list.size) {
                            val immutableListClass = runCatching {
                                trayData.javaClass.classLoader?.loadClass("com.google.common.collect.ImmutableList")
                            }.getOrNull()
                            val copyOf = immutableListClass?.declaredMethods?.firstOrNull { m ->
                                m.name == "copyOf" && m.parameterTypes.size == 1 && Collection::class.java.isAssignableFrom(m.parameterTypes[0])
                            }
                            if (copyOf != null) {
                                field.set(trayData, copyOf.invoke(null, filtered))
                                Logger.printDebug { "HideSuggestedStories: Filtered ${list.size - filtered.size} suggested story buckets" }
                            }
                        }
                        break
                    }
                }
            }
        })
    }
}
