package io.github.nexalloy.morphe.facebook.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.strings
import io.github.nexalloy.patch
import java.lang.reflect.Field
import java.lang.reflect.Modifier

private val AD_ROLES = setOf(
    "SEARCH_ADS",
    "DEPENDENT_SEARCH_ADS",
    "LATE_DEPENDENT_SEARCH_ADS",
    "TOP_POSITION_SEARCH_ADS",
    "TOP_POSITION_SHOPPABLE_ADS",
    "MARKETPLACE_SEARCH_ADS",
    "MARKETPLACE_BOOSTED_LISTING_SEARCH_ADS",
    "SEARCH_ADS_DISCOVERY_HEADER",
    "SEARCH_ADS_FLOATING_SEE_MORE"
)

/**
 * Matches SERP module list converters returning an ImmutableList of search result modules.
 * Anchored via the four kept role names in the module class's own ad set initializer.
 */
val searchModuleListConverters = findMethodListDirect {
    val moduleClass = findMethod {
        matcher {
            name = "<clinit>"
            strings(
                "SEARCH_ADS",
                "DEPENDENT_SEARCH_ADS",
                "LATE_DEPENDENT_SEARCH_ADS",
                "TOP_POSITION_SEARCH_ADS"
            )
        }
    }.first().declaredClass!!

    findMethod {
        matcher {
            returnType = "com.google.common.collect.ImmutableList"
            addInvoke {
                declaredClass(moduleClass.name)
                name = "<init>"
            }
        }
    }
}

/**
 * Hide sponsored search results:
 * Removes ads, sponsored posts, and promoted modules from search results.
 */
val HideSponsoredSearchResults = patch(
    name = "Hide sponsored search results",
    description = "Removes the ads from Facebook's search results, sponsored posts, and ad cards between search results.",
) {
    val copyOfMethod = runCatching {
        classLoader.loadClass("com.google.common.collect.ImmutableList").declaredMethods.firstOrNull { m ->
            m.name == "copyOf" && m.parameterTypes.size == 1 && Collection::class.java.isAssignableFrom(m.parameterTypes[0])
        }
    }.getOrNull()

    var cachedRoleField: Field? = null
    var roleFieldResolved = false

    fun findRoleField(item: Any): Field? {
        if (roleFieldResolved) return cachedRoleField
        roleFieldResolved = true
        cachedRoleField = item.javaClass.declaredFields.firstOrNull { f ->
            !Modifier.isStatic(f.modifiers) && f.type.isEnum &&
                runCatching {
                    (f.type.enumConstants as? Array<*>)?.any { (it as? Enum<*>)?.name == "SEARCH_ADS" } == true
                }.getOrDefault(false)
        }?.apply { isAccessible = true }
        return cachedRoleField
    }

    fun isAdModule(item: Any?): Boolean {
        if (item == null) return false
        val field = findRoleField(item) ?: return false
        val roleEnum = runCatching { field.get(item) as? Enum<*> }.getOrNull() ?: return false
        return roleEnum.name in AD_ROLES
    }

    val hook = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            if (!FacebookSettings.isEnabled(FacebookSettings.KEY_HIDE_SPONSORED_SEARCH_RESULTS, true)) return
            val list = param.result as? List<*> ?: return
            if (list.isEmpty() || list.none(::isAdModule)) return

            val filtered = list.filterNot(::isAdModule)
            param.result = if (copyOfMethod != null) {
                copyOfMethod.invoke(null, filtered)
            } else {
                filtered
            }
            Logger.printDebug { "HideSponsoredSearchResults: Filtered ${list.size - filtered.size} ad modules" }
        }
    }

    runCatching {
        ::searchModuleListConverters.dexMethodList.forEach { converter ->
            runCatching { converter.hookMethod(hook) }
        }
    }
}
