package io.github.nexalloy.morphe.threads.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.patch
import java.lang.reflect.Method
import java.lang.reflect.Modifier

val HideThreadsAds = patch(
    name = "Hide ads",
    description = "Removes sponsored and ad posts from Threads feed before they are cached or rendered.",
    use = true
) {
    val mediaClass = runCatching { classLoader.loadClass(MEDIA_CLASS) }.getOrNull()
    var cachedMediaGetter: Method? = null
    var cachedIsAdMethod: Method? = null

    fun getMediaFromItem(item: Any?): Any? {
        if (item == null || mediaClass == null) return null
        if (cachedMediaGetter != null) {
            return runCatching { cachedMediaGetter!!.invoke(item) }.getOrNull()
        }
        for (m in item.javaClass.declaredMethods) {
            if (m.returnType == mediaClass && m.parameterTypes.isEmpty() && !Modifier.isStatic(m.modifiers)) {
                m.isAccessible = true
                cachedMediaGetter = m
                return runCatching { m.invoke(item) }.getOrNull()
            }
        }
        return null
    }

    fun isMediaAd(media: Any?): Boolean {
        if (media == null || mediaClass == null) return false
        if (cachedIsAdMethod != null) {
            return runCatching { cachedIsAdMethod!!.invoke(media) as? Boolean ?: false }.getOrDefault(false)
        }
        for (m in mediaClass.declaredMethods) {
            if (m.returnType == java.lang.Boolean.TYPE && m.parameterTypes.isEmpty() && !Modifier.isStatic(m.modifiers)) {
                m.isAccessible = true
                val res = runCatching { m.invoke(media) as? Boolean }.getOrNull()
                // If it successfully returns a boolean, it is candidate for ad check
                // We test known ad check method or heuristic
                if (m.name.equals("isAd", ignoreCase = true) || m.name.equals("isSponsored", ignoreCase = true)) {
                    cachedIsAdMethod = m
                    return res ?: false
                }
            }
        }
        // Fallback: check any boolean method returning true on sponsored or checking field "injected"
        for (m in mediaClass.declaredMethods) {
            if (m.returnType == java.lang.Boolean.TYPE && m.parameterTypes.isEmpty() && !Modifier.isStatic(m.modifiers)) {
                m.isAccessible = true
                val res = runCatching { m.invoke(media) as? Boolean }.getOrNull() ?: false
                if (res) {
                    cachedIsAdMethod = m
                    return true
                }
            }
        }
        return false
    }

    FeedPageMergeFingerprint.hookMethod(object : XC_MethodHook() {
        @Suppress("UNCHECKED_CAST")
        override fun beforeHookedMethod(param: MethodHookParam) {
            val list = param.args.getOrNull(4) as? List<Any?> ?: return
            if (list.isEmpty()) return

            var hasAd = false
            for (item in list) {
                val media = getMediaFromItem(item)
                if (media != null && isMediaAd(media)) {
                    hasAd = true
                    break
                }
            }

            if (!hasAd) return

            Logger.printDebug { "Threads: Filtering ad posts from feed page" }

            val filtered = list.filter { item ->
                val media = getMediaFromItem(item)
                !(media != null && isMediaAd(media))
            }

            param.args[4] = filtered
        }
    })
}
