package io.github.nexalloy.morphe.facebook.feed

import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch

private const val BUMPER_COMPONENT = "NTFeedStoryBumperComponent"

internal object BumperComponentFingerprint : Fingerprint(
    strings = listOf(BUMPER_COMPONENT)
)

val HidePostPrompts = patch(
    name = "Hide post prompts",
    description = "Removes the strip Facebook adds to some posts, like suggestions and prompts.",
) {
    BumperComponentFingerprint.hookMethod(object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            if (FacebookSettings.isEnabled(FacebookSettings.KEY_HIDE_POST_PROMPTS, true)) {
                param.result = false
            }
        }
    })
}
