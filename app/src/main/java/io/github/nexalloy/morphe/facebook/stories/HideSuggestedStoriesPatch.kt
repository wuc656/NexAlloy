package io.github.nexalloy.morphe.facebook.stories

import app.morphe.extension.shared.Logger
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch

/**
 * Fingerprint matching Story Tray bucket conversions
 */
internal object StoryTrayBucketsFingerprint : Fingerprint(
    returnType = "Lcom/google/common/collect/ImmutableList;",
    strings = listOf("tray_session_id")
)

/**
 * Hide suggested stories:
 * Removes stories Facebook suggests from people and Pages you don't follow from the Stories tray.
 */
val HideSuggestedStories = patch(
    name = "Hide suggested stories",
    description = "Removes the stories Facebook suggests from people and Pages you don't follow, the ones marked Suggested in the Stories tray.",
) {
    runCatching {
        StoryTrayBucketsFingerprint.hookMethod {
            after { param ->
                if (!FacebookSettings.isEnabled(FacebookSettings.KEY_HIDE_SUGGESTED_STORIES, true)) return@after
                val list = param.result as? List<*> ?: return@after
                if (list.isEmpty()) return@after

                val filtered = list.filter { bucket ->
                    if (bucket == null) return@filter true
                    // Check if bucket has suggested flag or suggested label
                    val isSuggested = bucket.javaClass.declaredMethods.any { method ->
                        method.parameterTypes.isEmpty() &&
                        (method.name.contains("suggested", ignoreCase = true) || method.name.contains("isSuggested", ignoreCase = true)) &&
                        (runCatching { method.invoke(bucket) as? Boolean }.getOrNull() == true)
                    }
                    !isSuggested
                }

                if (filtered.size != list.size) {
                    val immutableListClass = runCatching {
                        classLoader.loadClass("com.google.common.collect.ImmutableList")
                    }.getOrNull()
                    val copyOf = immutableListClass?.declaredMethods?.firstOrNull { m ->
                        m.name == "copyOf" && m.parameterTypes.size == 1 && Collection::class.java.isAssignableFrom(m.parameterTypes[0])
                    }
                    param.result = if (copyOf != null) copyOf.invoke(null, filtered) else filtered
                    Logger.printDebug { "HideSuggestedStories: Filtered ${list.size - filtered.size} suggested story buckets" }
                }
            }
        }
    }
}
