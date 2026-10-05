package io.github.nexalloy.morphe.facebook.stories

import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch

internal object StoryAutoAdvanceFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Lcom/facebook/stories/model/StoryCard;", "LX/ACF;"),
    strings = listOf("StoryviewerAutoPlayNavigationController.moveToNextBucketOrThread")
)

val BlockStoryAutoAdvance = patch(
    name = "Stop Story auto-advance",
    description = "Keeps each Story on screen until you tap or swipe.",
) {
    runCatching {
        StoryAutoAdvanceFingerprint.hookMethod(object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (FacebookSettings.isEnabled(FacebookSettings.KEY_STOP_STORY_AUTO_ADVANCE, false)) {
                    param.result = null
                }
            }
        })
    }
}
