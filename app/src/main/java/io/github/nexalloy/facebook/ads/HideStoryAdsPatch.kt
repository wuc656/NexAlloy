package io.github.nexalloy.facebook.ads

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val HideStoryAds = patch(
    name = "Hide story ads",
    description = "Hides the ads in the Facebook app stories.",
) {
    runCatching {
        ::adsInsertionFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
    }
    runCatching {
        ::fetchMoreAdsFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
    }
}
