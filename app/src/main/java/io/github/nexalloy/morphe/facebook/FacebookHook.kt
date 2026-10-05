package io.github.nexalloy.morphe.facebook

import io.github.nexalloy.patch
import io.github.nexalloy.morphe.facebook.ads.HideSponsoredPosts
import io.github.nexalloy.morphe.facebook.ads.BlockAdPrefetch
import io.github.nexalloy.morphe.facebook.ads.DisableAudienceNetwork
import io.github.nexalloy.morphe.facebook.ads.HideSponsoredStories
import io.github.nexalloy.morphe.facebook.ads.HideSponsoredReels
import io.github.nexalloy.morphe.facebook.ads.HideAffiliateLinks
import io.github.nexalloy.morphe.facebook.notifications.BlockPromotionalNotifications
import io.github.nexalloy.morphe.facebook.ads.BlockAdTelemetry
import io.github.nexalloy.morphe.facebook.settings.FacebookSettingsHook

val Hushfacebook = patch(
    name = "Hushfacebook",
    description = "Complete Facebook ad blocking and decluttering suite with in-app settings (long-press Facebook logo)."
) {
    // 1. Initialize Facebook Settings system and logo long press hook
    FacebookSettingsHook.initialize(this)

    // 2. Execute all sub-patches
    val subPatches = listOf(
        HideSponsoredPosts,
        BlockAdPrefetch,
        DisableAudienceNetwork,
        HideSponsoredStories,
        HideSponsoredReels,
        HideAffiliateLinks,
        BlockPromotionalNotifications,
        BlockAdTelemetry
    )

    subPatches.forEach { p ->
        runCatching {
            p.run.invoke(this)
        }.onFailure { err ->
            app.morphe.extension.shared.Logger.printException({ "Failed to initialize sub-patch ${p.name}" }, err)
        }
    }
}

val FacebookPatches = arrayOf(Hushfacebook)
