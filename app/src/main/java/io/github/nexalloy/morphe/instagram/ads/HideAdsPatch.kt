package io.github.nexalloy.morphe.instagram.ads

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val HideInstagramAds = patch(
    name = "HushGram Hide ads",
    description = "Hides sponsored posts, reels and stories without leaving gaps (HushGram).",
) {
    ::adInjectorFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
}
