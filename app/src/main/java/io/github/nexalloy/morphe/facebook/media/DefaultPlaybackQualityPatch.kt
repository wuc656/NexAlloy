package io.github.nexalloy.morphe.facebook.media

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Fingerprint matching the method holding "HeroServicePlayer.setCustomQualityInternal".
 */
internal object HeroSetCustomQualityFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("HeroServicePlayer.setCustomQualityInternal")
)

/**
 * Fingerprint matching the DASH format evaluator which takes AbrContextAwareConfiguration in constructor.
 */
internal object FormatEvaluatorFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("AbrContextAwareConfiguration")
)

val DefaultPlaybackQuality = patch(
    name = "Default playback quality",
    description = "Plays videos, reels and video stories at the quality you choose in Hushfacebook's settings.",
    use = true
) {
    HeroSetCustomQualityFingerprint.hookMethod(object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            if (!FacebookSettings.isEnabled(FacebookSettings.KEY_DEFAULT_PLAYBACK_QUALITY, false)) return

            val savedQualityStr = FacebookSettings.getString(
                FacebookSettings.KEY_PLAYBACK_QUALITY,
                PlaybackQuality.HIGHEST.fileValue
            )
            val preferredQuality = PlaybackQuality.fromFile(savedQualityStr)
            if (preferredQuality == PlaybackQuality.AUTO) return

            // If the incoming argument is already a specific custom label or our token
            val currentArg = param.args.firstOrNull() as? String
            Logger.printDebug { "DefaultPlaybackQuality: HeroServicePlayer.setCustomQualityInternal called with arg: $currentArg, preferred: ${preferredQuality.fileValue}" }

            // When Facebook passes null or auto, or when starting playback, inject preferred quality if applicable
            if (currentArg == null || currentArg.equals("auto", ignoreCase = true)) {
                // If setCustomQualityInternal takes a quality label, inject our preferred quality label or highest
                if (preferredQuality == PlaybackQuality.HIGHEST) {
                    param.args[0] = "1080p" // Evaluator falls back to best matching track if 1080p is unavailable
                } else {
                    param.args[0] = preferredQuality.fileValue
                }
                Logger.printDebug { "DefaultPlaybackQuality: Overriding quality to ${param.args[0]}" }
            }
        }
    })
}
