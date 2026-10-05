package io.github.nexalloy.morphe.facebook.reels

import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch

internal object ReelPromptClinitFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("INTERESTED_OR_NOT_INTERESTED_BUMPER", "TUNE_YOUR_ALGORITHM")
)

val HideReelPrompts = patch(
    name = "Hide reel interest prompts",
    description = "Removes the 'Are you interested in this reel?' prompt from reels.",
) {
    runCatching {
        ReelPromptClinitFingerprint.hookMethod(object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (FacebookSettings.isEnabled(FacebookSettings.KEY_HIDE_REEL_PROMPTS, true)) {
                    param.result = false
                }
            }
        })
    }
}
