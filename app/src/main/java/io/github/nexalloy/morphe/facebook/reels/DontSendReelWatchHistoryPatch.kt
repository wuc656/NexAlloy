package io.github.nexalloy.morphe.facebook.reels

import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch

internal object ReelWatchHistoryFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("FbShortsSeenStateMutation", "video_ids")
)

val DontSendReelWatchHistory = patch(
    name = "Don't send reel watch history",
    description = "Stops sending Facebook the list of reels you've watched.",
) {
    ReelWatchHistoryFingerprint.hookMethod(object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            if (FacebookSettings.isEnabled(FacebookSettings.KEY_DONT_SEND_REEL_WATCH_HISTORY, false)) {
                param.result = null
            }
        }
    })
}
