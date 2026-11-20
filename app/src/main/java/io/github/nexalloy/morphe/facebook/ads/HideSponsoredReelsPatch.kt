package io.github.nexalloy.morphe.facebook.ads

import app.morphe.extension.shared.Logger
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.strings
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

internal object FbShortsAdPoolVendFingerprint : Fingerprint(
    custom = {
        declaredClass {
            usingStrings("FBShortsSponsoredPool.")
        }
        paramTypes(null, null, "java.util.List", "int")
    }
)

internal object FbShortsAdPoolVendValidateFingerprint : Fingerprint(
    custom = {
        declaredClass {
            usingStrings("FBShortsSponsoredPool.")
        }
        paramTypes("com.facebook.auth.usersession.FbUserSession", null, null, null, "java.util.List", "java.util.List", "int", "boolean")
    }
)

/**
 * Matches ProfileReelsAsyncAdsQuery senders (void methods in <=587, or callers of static builder in 588+).
 */
val profileReelAdSenders = findMethodListDirect {
    val holding = findMethod {
        matcher {
            strings("ProfileReelsAsyncAdsQuery")
        }
    }
    val voidDirect = holding.filter { it.returnTypeName == "void" }
    if (voidDirect.isNotEmpty()) return@findMethodListDirect voidDirect

    val builder = holding.first { it.returnTypeName != "void" }
    findMethod {
        matcher {
            returnType = "void"
            addInvoke {
                declaredClass(builder.declaredClassName)
                name = builder.name
            }
        }
    }
}

val HideSponsoredReels = patch(
    name = "Hide sponsored reels",
    description = "Removes ads from Reels and Watch (including profile/Page Reels tabs), product banners over a reel, and ads inside a video.",
) {
    val fingerprints = listOf(
        ReelsMaybeInsertAdsFingerprint,
        ReelsRealtimeIntentFingerprint,
        ReelsSfdAdsFingerprint,
        PreEofIfuInjectorFingerprint
    )

    fingerprints.forEach { fp ->
        runCatching {
            fp.hookMethod {
                before { param ->
                    if (!FacebookSettings.isEnabled(FacebookSettings.KEY_HIDE_SPONSORED_REELS, true)) return@before
                    Logger.printDebug { "Hide sponsored reels: Blocked ad insertion" }
                    param.result = null // Returns void immediately
                }
            }
        }
    }

    // VideoHomeSponsoredPool (-WVCDF-NO-AD) and FbShortsSponsoredPool (FBShortsSponsoredPool.)
    listOf(
        ReelsAdPoolVend1Fingerprint,
        ReelsAdPoolVend2Fingerprint,
        FbShortsAdPoolVendFingerprint,
        FbShortsAdPoolVendValidateFingerprint
    ).forEach { fp ->
        runCatching {
            fp.hookMethod {
                before { param ->
                    if (!FacebookSettings.isEnabled(FacebookSettings.KEY_HIDE_SPONSORED_REELS, true)) return@before
                    Logger.printDebug { "Hide sponsored reels: Held ad pool vend to null" }
                    param.result = null
                }
            }
        }
    }

    // Profile / Page Reels tab ad fetch (ProfileReelsAsyncAdsQuery)
    runCatching {
        ::profileReelAdSenders.dexMethodList.forEach { sender ->
            runCatching {
                sender.hookMethod {
                    before { param ->
                        if (!FacebookSettings.isEnabled(FacebookSettings.KEY_HIDE_SPONSORED_REELS, true)) return@before
                        Logger.printDebug { "Hide sponsored reels: Held ProfileReelsAsyncAdsQuery" }
                        param.result = null
                    }
                }
            }
        }
    }
}
