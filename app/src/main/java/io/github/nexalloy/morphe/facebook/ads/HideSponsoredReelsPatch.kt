package io.github.nexalloy.morphe.facebook.ads

import app.morphe.extension.shared.Logger
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.patch

val HideSponsoredReels = patch(
    name = "Hide sponsored reels",
    description = "Removes ads from Reels and Watch, including product banners over a reel and ads inside a video.",
) {
    val fingerprints = listOf(
        Fingerprint(returnType = "V", strings = listOf("VideoHomeDataControllerImpl.maybeInsertAds")),
        Fingerprint(returnType = "V", strings = listOf("VideoHomeDataControllerAdsUtil.maybeInsertFbShortsRealtimeIntentItem")),
        Fingerprint(returnType = "V", strings = listOf("VideoHomeDataControllerSfdAdsUtil"))
    )

    fingerprints.forEach { fp ->
        fp.hookMethod {
            before { param ->
                Logger.printDebug { "Hide sponsored reels: Blocked ad insertion" }
                param.result = null // Returns void immediately
            }
        }
    }
}
