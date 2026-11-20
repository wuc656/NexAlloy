/*
 * Copyright 2026 HushThreads contributors
 * https://github.com/SysAdminDoc/HushThreads
 */
package io.github.nexalloy.morphe.threads.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
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
    description = "Removes sponsored and ad posts from Threads feed (including For You cache writes, single-ad inserts, and permalink replies) before they are cached or rendered (HushThreads).",
    use = true
) {
    // 1. Guard client-side ad injector
    runCatching {
        ::adInjectorFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
    }

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
            mediaClass = classLoader.loadClass(MEDIA_CLASS)
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

    val filterListHook = object : XC_MethodHook() {
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
            Logger.printDebug { "HushThreads: Filtered ${beforeSize - filtered.size} ad posts from feed cache list" }
        }
    }

    // 2. Filter fetched page merge
    runCatching {
        ::feedPageMergeMethod.hookMethod(filterListHook)
    }.onFailure {
        Logger.printDebug { "HushThreads: Failed to hook feedPageMergeMethod: ${it.message}" }
    }

    // 3. Filter additional For You suspend list writes (refresh, paging, append, rebuild)
    runCatching {
        ::forYouListWriteMethods.dexMethodList.forEach { method ->
            method.hookMethod(filterListHook)
        }
    }.onFailure {
        Logger.printDebug { "HushThreads: Failed to hook forYouListWriteMethods: ${it.message}" }
    }

    // 4. Block single-item writes in BarcelonaFeedCache when the item is an ad
    runCatching {
        val doneUnit = runCatching { ::forYouDoneUnitField.field.apply { isAccessible = true }.get(null) }.getOrNull()
        if (doneUnit != null) {
            val itemWrites = ::forYouItemWriteMethods.dexMethodList
            itemWrites.forEach { method ->
                method.hookMethod(object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        initMethods()
                        val item = param.args.firstOrNull() ?: return
                        if (isAdItem(item)) {
                            param.result = doneUnit
                            Logger.printDebug { "HushThreads: Blocked single ad item write to feed cache" }
                        }
                    }
                })
            }
        }
    }.onFailure {
        Logger.printDebug { "HushThreads: Failed to hook forYouItemWriteMethods: ${it.message}" }
    }

    // 5. Block For You standalone single-ad insert into an existing cached page
    runCatching {
        val noPageCode = runCatching { ::forYouAdInsertNoPageField.field.apply { isAccessible = true }.get(null) }.getOrNull()
        if (noPageCode != null) {
            var cachedInsertItemField: Field? = null
            var insertFieldResolved = false
            ::forYouAdInsertMethod.hookMethod(object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    initMethods()
                    var hit = param.args.any { arg -> arg != null && isAdItem(arg) }
                    if (!hit) {
                        val holder = param.args.firstOrNull { it != null && !it.javaClass.isPrimitive && it !is Number && it !is String }
                        if (holder != null) {
                            if (!insertFieldResolved) {
                                insertFieldResolved = true
                                cachedInsertItemField = holder.javaClass.declaredFields.firstOrNull { f ->
                                    if (Modifier.isStatic(f.modifiers) || f.type.isPrimitive || f.type == String::class.java) return@firstOrNull false
                                    f.isAccessible = true
                                    val candidate = runCatching { f.get(holder) }.getOrNull() ?: return@firstOrNull false
                                    isAdItem(candidate) || getMediaFromItem(candidate) != null
                                }?.apply { isAccessible = true }
                            }
                            val item = runCatching { cachedInsertItemField?.get(holder) }.getOrNull()
                            if (item != null && isAdItem(item)) {
                                hit = true
                            }
                        }
                    }
                    if (hit) {
                        param.result = noPageCode
                        Logger.printDebug { "HushThreads: Blocked standalone For You ad insert" }
                    }
                }
            })
        }
    }.onFailure {
        Logger.printDebug { "HushThreads: Failed to hook forYouAdInsertMethod: ${it.message}" }
    }

    // 6. Clear permalink ads from opened post replies
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
