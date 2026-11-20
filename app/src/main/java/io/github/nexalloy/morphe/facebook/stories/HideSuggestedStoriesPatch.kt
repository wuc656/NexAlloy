package io.github.nexalloy.morphe.facebook.stories

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.strings
import io.github.nexalloy.patch
import java.lang.reflect.Modifier

private val SUGGESTED_BUCKET_TYPES = setOf(
    "PYMK_PROFILE_FORWARD_STORY",
    "PYMK_STORY",
    "CONTACT_IMPORTER_STORY"
)

internal object StoryPostProcessFingerprint : Fingerprint(
    strings = listOf("StoriesTrayLightFetchControllerQueryOps.postProcessResult")
)

/**
 * StoryBucket tree getter reading "is_story_bucket_suggested" hash.
 */
val storyBucketIsSuggestedMethod = findMethodDirect {
    findMethod {
        matcher {
            returnType = "boolean"
            paramCount = 0
            usingNumbers("is_story_bucket_suggested".hashCode().toLong())
        }
    }.first { !Modifier.isStatic(it.modifiers) }
}

/**
 * StoryBucket getter returning the bucket type enum ("PYMK_STORY", "CONTACT_IMPORTER_STORY").
 */
val storyBucketTypeMethod = findMethodDirect {
    val bucketTypeEnum = findMethod {
        matcher {
            name = "<clinit>"
            strings("PYMK_STORY", "CONTACT_IMPORTER_STORY")
        }
    }.first().declaredClassName

    findMethod {
        matcher {
            returnType = bucketTypeEnum
            paramCount = 0
            usingNumbers("story_bucket_type".hashCode().toLong())
        }
    }.first { !Modifier.isStatic(it.modifiers) }
}

val HideSuggestedStories = patch(
    name = "Hide suggested stories",
    description = "Removes the stories Facebook suggests from people and Pages you don't follow, the ones marked Suggested in the Stories tray.",
) {
    val isSuggestedMethod = runCatching { ::storyBucketIsSuggestedMethod.method.apply { isAccessible = true } }.getOrNull()
    val bucketTypeMethod = runCatching { ::storyBucketTypeMethod.method.apply { isAccessible = true } }.getOrNull()
    val copyOfMethod = runCatching {
        classLoader.loadClass("com.google.common.collect.ImmutableList").declaredMethods.firstOrNull { m ->
            m.name == "copyOf" && m.parameterTypes.size == 1 && Collection::class.java.isAssignableFrom(m.parameterTypes[0])
        }
    }.getOrNull()

    fun isSuggestedBucket(bucket: Any?): Boolean {
        if (bucket == null) return false
        if (isSuggestedMethod != null && isSuggestedMethod.declaringClass.isInstance(bucket)) {
            if (runCatching { isSuggestedMethod.invoke(bucket) as? Boolean }.getOrNull() == true) {
                return true
            }
        }
        if (bucketTypeMethod != null && bucketTypeMethod.declaringClass.isInstance(bucket)) {
            val typeEnum = runCatching { bucketTypeMethod.invoke(bucket) as? Enum<*> }.getOrNull()
            if (typeEnum != null && typeEnum.name in SUGGESTED_BUCKET_TYPES) {
                return true
            }
        }
        return false
    }

    runCatching {
        StoryPostProcessFingerprint.hookMethod(object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (!FacebookSettings.isEnabled(FacebookSettings.KEY_HIDE_SUGGESTED_STORIES, true)) return
                val trayData = param.result ?: return
                for (field in trayData.javaClass.declaredFields) {
                    if (List::class.java.isAssignableFrom(field.type)) {
                        field.isAccessible = true
                        val list = runCatching { field.get(trayData) as? List<*> }.getOrNull() ?: continue
                        if (list.isEmpty() || list.none(::isSuggestedBucket)) continue

                        val filtered = list.filterNot(::isSuggestedBucket)
                        if (copyOfMethod != null) {
                            field.set(trayData, copyOfMethod.invoke(null, filtered))
                            Logger.printDebug { "HideSuggestedStories: Filtered ${list.size - filtered.size} suggested story buckets" }
                        }
                        break
                    }
                }
            }
        })
    }
}
