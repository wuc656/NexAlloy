package io.github.nexalloy.morphe.facebook.ads

import app.morphe.extension.shared.Logger
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.patch

internal object ReelsMaybeInsertAdsFingerprint : Fingerprint(returnType = "V", strings = listOf("VideoHomeDataControllerImpl.maybeInsertAds"))
internal object ReelsRealtimeIntentFingerprint : Fingerprint(returnType = "V", strings = listOf("VideoHomeDataControllerAdsUtil.maybeInsertFbShortsRealtimeIntentItem"))
internal object ReelsSfdAdsFingerprint : Fingerprint(returnType = "V", strings = listOf("VideoHomeDataControllerSfdAdsUtil"))
internal object PreEofIfuInjectorFingerprint : Fingerprint(returnType = "V", strings = listOf("PreEofIfuSectionAdapter"))

internal object ReelsAdPoolVend1Fingerprint : Fingerprint(
    custom = {
        usingStrings("-WVCDF-NO-AD")
        paramTypes("com.facebook.auth.usersession.FbUserSession", null, null, null, "java.util.List", "java.util.List", "int", "boolean")
    }
)

internal object ReelsAdPoolVend2Fingerprint : Fingerprint(
    custom = {
        usingStrings("-WVCDF-NO-AD")
        paramTypes(null, null, "java.util.List", "int")
    }
)

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
                if (!io.github.nexalloy.morphe.facebook.settings.FacebookSettings.isEnabled(
                        io.github.nexalloy.morphe.facebook.settings.FacebookSettings.KEY_HIDE_SPONSORED_REELS, true
                    )) return@before
                Logger.printDebug { "Hide sponsored reels: Blocked ad insertion" }
                param.result = null // Returns void immediately
            }
        }
    }

    // VideoHomeSponsoredPool (-WVCDF-NO-AD, v0.7.2 e100e0a)
    listOf(ReelsAdPoolVend1Fingerprint, ReelsAdPoolVend2Fingerprint).forEach { fp ->
        fp.hookMethod {
            before { param ->
                if (!io.github.nexalloy.morphe.facebook.settings.FacebookSettings.isEnabled(
                        io.github.nexalloy.morphe.facebook.settings.FacebookSettings.KEY_HIDE_SPONSORED_REELS, true
                    )) return@before
                Logger.printDebug { "Hide sponsored reels: Held VideoHomeSponsoredPool ad vend to null" }
                param.result = null
            }
        }
    }
}

