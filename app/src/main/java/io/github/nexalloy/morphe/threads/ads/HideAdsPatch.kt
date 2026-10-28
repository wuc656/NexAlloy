/*
 * Copyright 2026 HushThreads contributors
 * https://github.com/SysAdminDoc/HushThreads
 */
package io.github.nexalloy.morphe.threads.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.patch
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

private val AD_UNIT_TYPES = setOf(
    "AD",
    "AD4AD",
    "INTENT_AWARE_AD_PIVOT",
    "STAND_ALONE_MULTI_AD_PIVOT",
    "ADS_FEEDBACK_INTERFACE",
    "ADS_FEEDBACK_INTERFACE_INTERESTS_PICKER",
    "ADS_FEEDBACK_INTERFACE_REPETITION"
)

val HideThreadsAds = patch(
    name = "HushThreads Hide ads",
    description = "Removes sponsored and ad posts from Threads feed before they are cached or rendered (HushThreads).",
    use = true
) {
    var mediaClass: Class<*>? = null
    var cachedMediaGetter: Method? = null
    var cachedUnitTypeField: Field? = null
    var unitTypeFieldResolved = false
    var resolved = false
    // Resolve Media.isAd via DexKit during patch initialization (while dexkit bridge is open)
    var cachedIsAdMethod: Method? = runCatching {
        ::mediaIsAdMethod.method.apply {
            isAccessible = true
            Logger.printInfo { "HushThreads: Resolved Media.isAd via DexKit: $name" }
        }
    }.getOrNull()

    fun initMethods() {
        if (resolved) return
        resolved = true
        try {
            mediaClass = classLoader.loadClass("com.instagram.feed.media.Media")
            if (cachedIsAdMethod == null) {
                for (m in mediaClass!!.declaredMethods) {
                    if (m.returnType == java.lang.Boolean.TYPE && m.parameterTypes.isEmpty() && !Modifier.isStatic(m.modifiers)) {
                        if (m.name == "DKT") {
                            m.isAccessible = true
                            cachedIsAdMethod = m
                            Logger.printInfo { "HushThreads: Found Media isAd method by DKT symbol: ${m.name}" }
                            break
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Logger.printException({ "HushThreads: Failed to resolve Media.isAd method" }, t)
        }
    }

    fun isAdUnitType(item: Any?): Boolean {
        if (item == null) return false
        if (!unitTypeFieldResolved) {
            unitTypeFieldResolved = true
            cachedUnitTypeField = item.javaClass.declaredFields.firstOrNull { f ->
                !Modifier.isStatic(f.modifiers) && f.type.isEnum &&
                    runCatching {
                        val names = (f.type.enumConstants as? Array<*>)?.mapNotNull { (it as? Enum<*>)?.name }?.toSet().orEmpty()
                        "AD" in names && "THREAD" in names
                    }.getOrDefault(false)
            }?.apply { isAccessible = true }
        }
        val unitType = runCatching { cachedUnitTypeField?.get(item) as? Enum<*> }.getOrNull() ?: return false
        return unitType.name in AD_UNIT_TYPES
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

    fun isAdItem(item: Any?): Boolean {
        if (item == null) return false
        if (isAdUnitType(item)) return true
        val media = getMediaFromItem(item)
        return media != null && isAd(media)
    }

    runCatching {
        ::feedPageMergeMethod.hookMethod(object : XC_MethodHook() {
            @Suppress("UNCHECKED_CAST")
            override fun beforeHookedMethod(param: MethodHookParam) {
                initMethods()
                val listIndex = param.args.indexOfFirst { it is List<*> }
                if (listIndex == -1) return
                val list = param.args[listIndex] as? List<Any?> ?: return
                if (list.isEmpty()) return

                if (list.none { isAdItem(it) }) return

                val beforeSize = list.size
                val filtered = list.filterNot { isAdItem(it) }
                param.args[listIndex] = filtered
                Logger.printDebug { "HushThreads: Filtered ${beforeSize - filtered.size} ad posts from feed page" }
            }
        })
    }.onFailure {
        Logger.printDebug { "HushThreads: Failed to hook feedPageMergeMethod: ${it.message}" }
    }

    runCatching {
        ::permalinkAdResultConstructor.hookMethod(object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.args[0] = java.util.Collections.emptyList<Any?>()
                param.args[1] = 0
                Logger.printDebug { "HushThreads: Cleared permalink ads from opened post replies" }
            }
        })
    }.onFailure {
        Logger.printDebug { "HushThreads: Failed to hook permalinkAdResultConstructor: ${it.message}" }
    }
}
