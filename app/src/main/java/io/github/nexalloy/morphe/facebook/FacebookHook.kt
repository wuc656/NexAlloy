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
import io.github.nexalloy.morphe.facebook.ads.HideSponsoredProfilePosts
import io.github.nexalloy.morphe.facebook.feed.HidePostPrompts
import io.github.nexalloy.morphe.facebook.feed.HideMetaAiQuestions
import io.github.nexalloy.morphe.facebook.feed.HideFeedsHeader
import io.github.nexalloy.morphe.facebook.settings.FacebookSettingsHook

val FacebookSettingsPatch = patch(
    name = "Facebook in-app settings",
    description = "Enables long-pressing the Facebook logo to open the in-app settings popup.",
    use = true
) {
    FacebookSettingsHook.initialize(this)
}

val FacebookPatches = arrayOf(
    FacebookSettingsPatch,
    HideSponsoredPosts,
    BlockAdPrefetch,
    DisableAudienceNetwork,
    HideSponsoredStories,
    HideSponsoredReels,
    HideAffiliateLinks,
    BlockPromotionalNotifications,
    BlockAdTelemetry,
    HideSponsoredProfilePosts,
    HidePostPrompts,
    HideMetaAiQuestions,
    HideFeedsHeader
)
