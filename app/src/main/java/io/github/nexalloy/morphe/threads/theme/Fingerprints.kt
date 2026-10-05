package io.github.nexalloy.morphe.threads.theme

import io.github.nexalloy.morphe.Fingerprint

internal const val BDS_THEME_STRING = "com.instagram.barcelona.bds.theme.BdsTheme.<anonymous> (BdsTheme.kt:41)"

/**
 * Fingerprint matching BdsTheme Compose theme lambda.
 */
internal object BdsThemeFingerprint : Fingerprint(
    custom = {
        usingStrings(BDS_THEME_STRING)
    }
)
