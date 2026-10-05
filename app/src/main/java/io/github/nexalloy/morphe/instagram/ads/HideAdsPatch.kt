package io.github.nexalloy.morphe.instagram.ads

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val HideInstagramAds = patch(
    name = "Hide ads",
    description = "Hides sponsored posts, reels and stories without leaving gaps.",
) {
    ::adInjectorFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
}
