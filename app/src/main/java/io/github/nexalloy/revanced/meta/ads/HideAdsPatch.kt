package io.github.nexalloy.revanced.meta.ads

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val HideAds = patch(
    name = "Hide ads",
) {
    // Instagram — method returns boolean, return true to mark as ad pod
    runCatching {
        ::igAdInjectorFingerprint.hookMethod(
            XC_MethodReplacement.returnConstant(true),
        )
    }

    // Threads — method returns void, replace with no-op
    runCatching {
        ::threadsAdInjectorFingerprint.hookMethod(
            XC_MethodReplacement.DO_NOTHING,
        )
    }
}