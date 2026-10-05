package io.github.nexalloy.morphe.facebook

import io.github.nexalloy.morphe.facebook.ads.HideSponsoredPosts
import io.github.nexalloy.morphe.facebook.ads.BlockAdPrefetch
import io.github.nexalloy.morphe.facebook.ads.DisableAudienceNetwork
import io.github.nexalloy.morphe.facebook.ads.HideSponsoredStories
import io.github.nexalloy.morphe.facebook.ads.HideSponsoredReels
import io.github.nexalloy.morphe.facebook.ads.HideAffiliateLinks

val FacebookPatches = arrayOf(HideSponsoredPosts, BlockAdPrefetch, DisableAudienceNetwork, HideSponsoredStories, HideSponsoredReels, HideAffiliateLinks)

