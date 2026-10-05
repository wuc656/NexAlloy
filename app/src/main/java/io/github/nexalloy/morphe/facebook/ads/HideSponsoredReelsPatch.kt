package io.github.nexalloy.morphe.facebook.ads

import app.morphe.extension.shared.Logger
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.patch

internal object ReelsMaybeInsertAdsFingerprint : Fingerprint(returnType = "V", strings = listOf("VideoHomeDataControllerImpl.maybeInsertAds"))
internal object ReelsRealtimeIntentFingerprint : Fingerprint(returnType = "V", strings = listOf("VideoHomeDataControllerAdsUtil.maybeInsertFbShortsRealtimeIntentItem"))
internal object ReelsSfdAdsFingerprint : Fingerprint(returnType = "V", strings = listOf("VideoHomeDataControllerSfdAdsUtil"))
internal object PreEofIfuInjectorFingerprint : Fingerprint(returnType = "V", strings = listOf("PreEofIfuSectionAdapter"))

val HideSponsoredReels = patch(
    name = "Hide sponsored reels",
    description = "Removes ads from Reels and Watch, including product banners over a reel and ads inside a video.",
) {
    val fingerprints = listOf(
        ReelsMaybeInsertAdsFingerprint,
        ReelsRealtimeIntentFingerprint,
        ReelsSfdAdsFingerprint,
        PreEofIfuInjectorFingerprint
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

