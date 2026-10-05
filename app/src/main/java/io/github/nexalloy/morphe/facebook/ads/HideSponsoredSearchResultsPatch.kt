package io.github.nexalloy.morphe.facebook.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch

private const val USER_SESSION = "Lcom/facebook/auth/usersession/FbUserSession;"
private const val GRAPHQL_RESULT = "Lcom/facebook/graphql/executor/GraphQLResult;"
private const val SEARCH_CONTEXT = "Lcom/facebook/search/results/model/SearchResultsMutableContext;"

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
 * Fingerprint matching search page builder methods taking:
 * (FbUserSession, GraphQLResult, SearchResultsMutableContext, ...)
 */
internal object SearchPageBuilderFingerprint : Fingerprint(
    parameters = listOf(USER_SESSION, GRAPHQL_RESULT, SEARCH_CONTEXT)
)

/**
 * Hide sponsored search results:
 * Removes ads, sponsored posts, and promoted modules from search results.
 */
val HideSponsoredSearchResults = patch(
    name = "Hide sponsored search results",
    description = "Removes the ads from Facebook's search results, sponsored posts, and ad cards between search results.",
) {
    SearchPageBuilderFingerprint.hookMethod(object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            if (!FacebookSettings.isEnabled(FacebookSettings.KEY_HIDE_SPONSORED_SEARCH_RESULTS, true)) return

            val page = param.result ?: return
            // Page contains module list in its fields.
            for (field in page.javaClass.declaredFields) {
                if (List::class.java.isAssignableFrom(field.type)) {
                    field.isAccessible = true
                    val list = runCatching { field.get(page) as? List<*> }.getOrNull() ?: continue
                    if (list.isEmpty()) continue

                    // Check if any element in list is an ad module
                    var hasAds = false
                    for (item in list) {
                        if (item == null) continue
                        val roleField = item.javaClass.declaredFields.firstOrNull { f ->
                            Enum::class.java.isAssignableFrom(f.type)
                        } ?: continue
                        roleField.isAccessible = true
                        val roleEnum = runCatching { roleField.get(item) as? Enum<*> }.getOrNull()
                        if (roleEnum != null && roleEnum.name in AD_ROLES) {
                            hasAds = true
                            break
                        }
                    }

                    if (hasAds) {
                        val filtered = list.filter { item ->
                            if (item == null) return@filter true
                            val roleField = item.javaClass.declaredFields.firstOrNull { f ->
                                Enum::class.java.isAssignableFrom(f.type)
                            } ?: return@filter true
                            roleField.isAccessible = true
                            val roleEnum = runCatching { roleField.get(item) as? Enum<*> }.getOrNull()
                            val isAd = roleEnum != null && roleEnum.name in AD_ROLES
                            !isAd
                        }

                        // Try to replace list field with filtered copy
                        runCatching {
                            // If ImmutableList is required, copy or keep
                            val immutableListClass = runCatching {
                                classLoader.loadClass("com.google.common.collect.ImmutableList")
                            }.getOrNull()

                            val copyOf = immutableListClass?.declaredMethods?.firstOrNull { m ->
                                m.name == "copyOf" && m.parameterTypes.size == 1 && Collection::class.java.isAssignableFrom(m.parameterTypes[0])
                            }

                            val replacement = if (copyOf != null) {
                                copyOf.invoke(null, filtered)
                            } else {
                                filtered
                            }

                            field.set(page, replacement)
                            Logger.printDebug { "HideSponsoredSearchResults: Filtered ad modules from search results page" }
                        }
                    }
                }
            }
        }
    })
}
