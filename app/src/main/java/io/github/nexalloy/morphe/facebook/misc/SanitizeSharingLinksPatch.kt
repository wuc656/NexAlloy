package io.github.nexalloy.morphe.facebook.misc

import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch

private val TRACKING_PARAMS = setOf(
    "fbclid", "mibextid", "extid", "sfnsn", "rdid", "share_url",
    "__tn__", "__so__", "__rv__", "_ft_", "refsrc", "fref", "refid",
    "hc_ref", "hc_location", "paipv", "wtsid", "_rdr", "_rdc"
)

internal fun sanitizeUrl(urlStr: String): String {
    if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) return urlStr
    return runCatching {
        val uri = Uri.parse(urlStr)
        val queryNames = uri.queryParameterNames
        if (queryNames.none { it in TRACKING_PARAMS }) return urlStr

        val builder = uri.buildUpon().clearQuery()
        for (name in queryNames) {
            if (name !in TRACKING_PARAMS) {
                for (value in uri.getQueryParameters(name)) {
                    builder.appendQueryParameter(name, value)
                }
            }
        }
        val cleaned = builder.build().toString()
        Logger.printDebug { "SanitizeSharingLinks: Cleaned tracking tags from $urlStr -> $cleaned" }
        cleaned
    }.getOrDefault(urlStr)
}

/**
 * Fingerprint matching ExternalShareTracker methods adding tracking tags
 */
internal object ExternalShareTrackerFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("mibextid")
)

/**
 * Sanitize sharing links:
 * Takes Facebook's tracking tags, such as mibextid, off links shared or copied.
 */
val SanitizeSharingLinks = patch(
    name = "Sanitize sharing links",
    description = "Takes Facebook's tracking tags, such as mibextid, off the links you share or copy.",
) {
    // 1. Hook ExternalShareTracker methods that generate the URL
    runCatching {
        ExternalShareTrackerFingerprint.hookMethod(object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (!FacebookSettings.isEnabled(FacebookSettings.KEY_SANITIZE_SHARING_LINKS, true)) return
                val link = param.result as? String ?: return
                param.result = sanitizeUrl(link)
            }
        })
    }

    // 2. Hook ClipboardManager.setPrimaryClip as a universal safety net for Copy Link
    val clipboardClass = ClipboardManager::class.java
    val setPrimaryClipMethod = clipboardClass.declaredMethods.firstOrNull { it.name == "setPrimaryClip" }
    if (setPrimaryClipMethod != null) {
        XposedBridge.hookMethod(setPrimaryClipMethod, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (!FacebookSettings.isEnabled(FacebookSettings.KEY_SANITIZE_SHARING_LINKS, true)) return
                val clip = param.args?.getOrNull(0) as? ClipData ?: return
                if (clip.itemCount > 0) {
                    val text = clip.getItemAt(0)?.text?.toString() ?: return
                    val sanitized = sanitizeUrl(text)
                    if (sanitized != text) {
                        param.args[0] = ClipData.newPlainText(clip.description.label, sanitized)
                        Logger.printDebug { "SanitizeSharingLinks: Cleaned copied clipboard link" }
                    }
                }
            }
        })
    }
}
