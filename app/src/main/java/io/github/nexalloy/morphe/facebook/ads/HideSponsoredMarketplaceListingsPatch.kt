package io.github.nexalloy.morphe.facebook.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy

private const val NETWORKING_TAG = "FBNetworkingModule_React_Native"
private const val READABLE_MAP_CLASS = "com.facebook.react.bridge.ReadableMap"

private val ADS_ONLY_QUERIES = setOf(
    "MarketplaceHomeFeedAdsQueryRendererQuery",
    "MarketplaceHomeFeedAdsPaginationQuery",
    "MarketplaceHomeFeedBoostedListingAdsQuery",
    "MarketplaceHomeFeedBoostedListingAdsPaginationQuery",
    "MarketplaceProductDetailsPageRelatedAdsDetailQuery",
    "MarketplacePDPBoostedListingAdsQuery",
    "MarketplacePDPPersonalizedAdsQuery"
)

private val THEMED_ADS_QUERIES = setOf(
    "MarketplaceHomeFeedThemedAdsFetch",
    "MarketplaceHomeFeedAdsThemedQuery"
)

private val ADS_WORD_REGEX = Regex("Ads(?![a-z])")

private val SKIP_VAR_PLAIN_REGEX = Regex(""""(shouldSkipAdRequest|shouldSkipBoostedListingAdRequest)"\s*:\s*false\b""")
private val SKIP_VAR_ENCODED_REGEX = Regex("""%22(shouldSkipAdRequest|shouldSkipBoostedListingAdRequest)%22%3A\s*false\b""", RegexOption.IGNORE_CASE)

internal fun isAdsOnlyMarketplaceQuery(queryName: String): Boolean =
    queryName in ADS_ONLY_QUERIES ||
        queryName in THEMED_ADS_QUERIES ||
        (queryName.startsWith("MarketplaceHomeFeed") && ADS_WORD_REGEX.containsMatchIn(queryName))

internal fun rewriteSkipAdVariables(body: String): String {
    var updated = SKIP_VAR_PLAIN_REGEX.replace(body) { """"${it.groupValues[1]}":true""" }
    updated = SKIP_VAR_ENCODED_REGEX.replace(updated) { """%22${it.groupValues[1]}%22%3Atrue""" }
    return updated
}

/**
 * Fingerprint matching React Native Networking module's sendRequest method
 */
internal object MarketplaceSendRequestFingerprint : Fingerprint(
    name = "sendRequest",
    custom = {
        usingStrings(NETWORKING_TAG)
    }
)

/**
 * Hide sponsored Marketplace listings:
 * Removes ads and boosted listings from Marketplace's feed and listing pages.
 */
val HideSponsoredMarketplaceListings = patch(
    name = "Hide sponsored Marketplace listings",
    description = "Removes the ads and boosted listings from Marketplace's feed and search results.",
) {
    val readableMapClass = runCatching { classLoader.loadClass(READABLE_MAP_CLASS) }.getOrNull()
    val getStringMethod = runCatching {
        readableMapClass?.getMethod("getString", String::class.java)
    }.getOrNull()

    fun wrapReadableMapWithBodyOverride(origMap: Any, mapInterface: Class<*>, bodyOverride: String?): Any =
        Proxy.newProxyInstance(mapInterface.classLoader, arrayOf(mapInterface)) { _, method, args ->
            if (method.name == "getString" && args != null && args.size == 1 && args[0] == "string") {
                return@newProxyInstance bodyOverride
            }
            try {
                method.invoke(origMap, *(args ?: emptyArray()))
            } catch (e: InvocationTargetException) {
                throw e.targetException
            }
        }

    runCatching {
        MarketplaceSendRequestFingerprint.hookMethod(object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (!FacebookSettings.isEnabled(FacebookSettings.KEY_HIDE_SPONSORED_MARKETPLACE, true)) return
                val args = param.args ?: return
                val mapInterface = readableMapClass ?: return
                val getString = getStringMethod ?: return

                val dataIdx = args.indexOfFirst { it != null && mapInterface.isInstance(it) }
                if (dataIdx < 0) return
                val dataMap = args[dataIdx] ?: return

                val trackingName = runCatching {
                    getString.invoke(dataMap, "trackingName") as? String
                }.getOrNull() ?: return
                val queryName = trackingName.removePrefix("RelayFBNetwork_")

                if (isAdsOnlyMarketplaceQuery(queryName)) {
                    Logger.printDebug { "HideSponsoredMarketplaceListings: Held ads-only query $queryName" }
                    param.args[dataIdx] = wrapReadableMapWithBodyOverride(dataMap, mapInterface, null)
                    return
                }

                if (queryName.startsWith("Marketplace")) {
                    val body = runCatching { getString.invoke(dataMap, "string") as? String }.getOrNull() ?: return
                    if (body.contains("shouldSkipAdRequest") || body.contains("shouldSkipBoostedListingAdRequest")) {
                        val rewritten = rewriteSkipAdVariables(body)
                        if (rewritten != body) {
                            Logger.printDebug { "HideSponsoredMarketplaceListings: Rewrote skip-ad flags in $queryName" }
                            param.args[dataIdx] = wrapReadableMapWithBodyOverride(dataMap, mapInterface, rewritten)
                        }
                    }
                }
            }
        })
    }
}
