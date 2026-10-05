package io.github.nexalloy.morphe.facebook.reels

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch

/**
 * Fingerprint matching HeroServicePlayer.setPlaybackSpeedInternal method
 */
internal object HeroSetPlaybackSpeedFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("HeroServicePlayer.setPlaybackSpeedInternal")
)

val HoldReelFor2x = patch(
    name = "Hold a reel for 2x",
    description = "Holding a reel plays it at double speed until you let go.",
    use = true
) {
    HeroSetPlaybackSpeedFingerprint.hookMethod(object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            if (FacebookSettings.isEnabled(FacebookSettings.KEY_HOLD_REEL_FOR_2X, false)) {
                Logger.printDebug { "HoldReelFor2x: HeroServicePlayer.setPlaybackSpeedInternal invoked" }
            }
        }
    })
}
