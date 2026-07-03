package io.github.nexalloy.revanced.meta.ads

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val HideAds = patch(
    name = "Hide ads",
) {
    // Try Instagram fingerprint (returns boolean)
    runCatching {
        ::igAdInjectorFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
    }

    // Try Threads fingerprint (returns void)
    runCatching {
        ::threadsAdInjectorFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
    }
}