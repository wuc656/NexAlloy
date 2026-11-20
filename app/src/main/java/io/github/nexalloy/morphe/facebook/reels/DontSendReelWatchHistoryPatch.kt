package io.github.nexalloy.morphe.facebook.reels

import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch

internal object ReelWatchHistoryFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("FbShortsSeenStateMutation")
)

internal object WatchedPositionMutationFingerprint : Fingerprint(
    isStatic = true,
    returnType = "V",
    strings = listOf("SfdVideoPositionWatchedMutation", "position_ms")
)

internal object VideoViewTimeLoggerFingerprint : Fingerprint(
    isStatic = true,
    returnType = "V",
    strings = listOf("client_vdd_position", "vvt")
)

val DontSendReelWatchHistory = patch(
    name = "Don't send reel watch history",
    description = "Stops sending Facebook the list of reels and watched video positions you've watched.",
) {
    val blockHook = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            if (FacebookSettings.isEnabled(FacebookSettings.KEY_DONT_SEND_REEL_WATCH_HISTORY, false)) {
                param.result = null
            }
        }
    }

    listOf(
        ReelWatchHistoryFingerprint,
        WatchedPositionMutationFingerprint,
        VideoViewTimeLoggerFingerprint
    ).forEach { fp ->
        runCatching { fp.hookMethod(blockHook) }
    }
}
