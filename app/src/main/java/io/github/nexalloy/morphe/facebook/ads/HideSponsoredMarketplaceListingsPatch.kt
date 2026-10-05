package io.github.nexalloy.morphe.facebook.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch

private const val NETWORKING_TAG = "FBNetworkingModule_React_Native"

private val ADS_ONLY_QUERIES = setOf(
    "MarketplaceHomeFeedAdsQueryRendererQuery",
    "MarketplaceHomeFeedAdsPaginationQuery",
    "MarketplaceHomeFeedBoostedListingAdsQuery",
    "MarketplaceHomeFeedBoostedListingAdsPaginationQuery"
)

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
 * Removes ads and boosted listings from Marketplace's feed and search results.
 */
val HideSponsoredMarketplaceListings = patch(
    name = "Hide sponsored Marketplace listings",
    description = "Removes the ads and boosted listings from Marketplace's feed and search results.",
) {
    MarketplaceSendRequestFingerprint.hookMethod(object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            if (!FacebookSettings.isEnabled(FacebookSettings.KEY_HIDE_SPONSORED_MARKETPLACE, true)) return

            val args = param.args ?: return
            // Parameter index 4 is ReadableMap data
            for (arg in args) {
                if (arg == null) continue
                val clazz = arg.javaClass
                // Look for getString(String) on ReadableMap
                val getStringMethod = runCatching {
                    clazz.getMethod("getString", String::class.java)
                }.getOrNull() ?: continue

                val trackingName = runCatching {
                    getStringMethod.invoke(arg, "trackingName") as? String
                }.getOrNull() ?: continue

                // Check if this is an ads-only query
                val queryName = trackingName.removePrefix("RelayFBNetwork_")
                if (ADS_ONLY_QUERIES.contains(queryName)) {
                    Logger.printDebug { "HideSponsoredMarketplaceListings: Blocked ads-only query: $queryName" }
                    // Prevent execution by setting empty result / cancelling
                    param.result = null
                    return
                }

                // If this is the home feed query, inspect and update body if possible
                if (queryName.contains("Marketplace") && queryName.contains("Feed")) {
                    val body = runCatching {
                        getStringMethod.invoke(arg, "string") as? String
                    }.getOrNull()
                    if (body != null && (body.contains("shouldSkipAdRequest") || body.contains("shouldSkipBoostedListingAdRequest"))) {
                        Logger.printDebug { "HideSponsoredMarketplaceListings: Feed query detected: $queryName" }
                    }
                }
            }
        }
    })
}
