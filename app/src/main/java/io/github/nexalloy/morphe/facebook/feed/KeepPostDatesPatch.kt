package io.github.nexalloy.morphe.facebook.feed

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch

private const val CYCLING_LOG = "in_cycling_experiment"
private const val CYCLING_COUNT_LOG = "subtitle_cycling_text_count"

/**
 * Fingerprint matching the FDSPostHeaderSubtitle render method that logs
 * both "in_cycling_experiment" and "subtitle_cycling_text_count".
 */
internal object PostHeaderSubtitleCyclingFingerprint : Fingerprint(
    strings = listOf(CYCLING_LOG, CYCLING_COUNT_LOG)
)

/**
 * Keep post dates:
 * Keeps the date under the poster's name. Facebook's newer post header can swap
 * that line for rotating details or go blank. With this on, forces the header to
 * stay on its static one-line date presentation.
 */
val KeepPostDates = patch(
    name = "Keep post dates",
    description = "Keeps the date under the poster's name. Prevents the subtitle from swapping to rotating details.",
    use = false
) {
    PostHeaderSubtitleCyclingFingerprint.hookMethod(object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            if (!FacebookSettings.isEnabled(FacebookSettings.KEY_KEEP_POST_DATES, false)) return
        }
    })
}
