/*
 * Copyright 2026 HushThreads contributors
 * https://github.com/SysAdminDoc/HushThreads
 */
package io.github.nexalloy.morphe.threads.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.patch
import java.lang.reflect.Method
import java.lang.reflect.Modifier

val HideThreadsAds = patch(
    name = "HushThreads Hide ads",
    description = "Removes sponsored and ad posts from Threads feed before they are cached or rendered (HushThreads).",
    use = true
) {
    var mediaClass: Class<*>? = null
    var cachedMediaGetter: Method? = null
    var cachedIsAdMethod: Method? = null
    var resolved = false

    fun initMethods() {
        if (resolved) return
        resolved = true
        try {
            mediaClass = classLoader.loadClass("com.instagram.feed.media.Media")
            val injectedMethod = InjectedAdCheckFingerprint.method
            val targetDeclaringClass = injectedMethod.declaringClass

            // Media's own ad check directly returns the injected check
            for (m in mediaClass!!.declaredMethods) {
                if (m.returnType == java.lang.Boolean.TYPE && m.parameterTypes.isEmpty() && !Modifier.isStatic(m.modifiers)) {
                    // Let's check if method name matches DKT or any candidate that invokes injectedMethod
                    if (m.name == "DKT") {
                        m.isAccessible = true
                        cachedIsAdMethod = m
                        Logger.printInfo { "HushThreads: Found Media isAd method by direct DKT symbol: ${m.name}" }
                        break
                    }
                }
            }

            // Fallback: search declared methods in Media with boolean return and no params
            if (cachedIsAdMethod == null) {
                for (m in mediaClass!!.declaredMethods) {
                    if (m.returnType == java.lang.Boolean.TYPE && m.parameterTypes.isEmpty() && !Modifier.isStatic(m.modifiers)) {
                        m.isAccessible = true
                        cachedIsAdMethod = m
                        break
                    }
                }
            }
        } catch (t: Throwable) {
            Logger.printException({ "HushThreads: Failed to resolve InjectedAdCheck / isAd method" }, t)
        }
    }

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

    fun isAd(media: Any?): Boolean {
        if (media == null || cachedIsAdMethod == null) return false
        return runCatching { cachedIsAdMethod!!.invoke(media) as? Boolean ?: false }.getOrDefault(false)
    }

    FeedPageMergeFingerprint.hookMethod(object : XC_MethodHook() {
        @Suppress("UNCHECKED_CAST")
        override fun beforeHookedMethod(param: MethodHookParam) {
            initMethods()
            val list = param.args.getOrNull(4) as? List<Any?> ?: return
            if (list.isEmpty()) return

            var hasAd = false
            for (item in list) {
                val media = getMediaFromItem(item)
                if (media != null && isAd(media)) {
                    hasAd = true
                    break
                }
            }

            if (!hasAd) return

            val beforeSize = list.size
            val filtered = list.filter { item ->
                val media = getMediaFromItem(item)
                !(media != null && isAd(media))
            }
            param.args[4] = filtered
            Logger.printDebug { "HushThreads: Filtered ${beforeSize - filtered.size} ad posts from feed page" }
        }
    })
}
