package io.github.nexalloy.morphe.twitch.ads

import android.net.Uri
import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch
import java.lang.reflect.Modifier

/**
 * aRandomHooman: Block live ads (Routes live streams through manifest proxy in ad-free region)
 */
val BlockLiveAds = patch(
    name = "Block live ads",
    description = "Routes live streams through a third-party manifest proxy in an ad-free region (aRandomHooman).",
    use = true
) {
    try {
        val proxyUrl = "https://lb-as.cdn-perfprod.com/live/"
        ::LiveManifestUrlBuilderFingerprint.hookMethod(object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val origUri = param.result as? Uri ?: return
                // Check if already routed or contains channel path
                val origStr = origUri.toString()
                if (origStr.contains(proxyUrl)) return

                val thisObj = param.thisObject ?: return
                // The stream name is stored in string field of the lambda
                var streamName: String? = null
                for (field in thisObj.javaClass.declaredFields) {
                    if (field.type == String::class.java && !Modifier.isStatic(field.modifiers)) {
                        field.isAccessible = true
                        val str = runCatching { field.get(thisObj) as? String }.getOrNull()
                        if (!str.isNullOrEmpty()) {
                            streamName = str
                            break
                        }
                    }
                }

                if (!streamName.isNullOrEmpty()) {
                    val newUrl = "$proxyUrl$streamName?allow_source=true&allow_audio_only=true&fast_bread=true&type=any&player=twitchweb"
                    Logger.printDebug { "Twitch: Block live ads routing to $newUrl" }
                    param.result = Uri.parse(newUrl)
                }
            }
        })
    } catch (t: Throwable) {
        Logger.printException({ "Failed to hook Block live ads" }, t)
    }
}

/**
 * aRandomHooman: Hide display ads (Hides banners, overlays, and in-feed sponsored items)
 */
val HideDisplayAds = patch(
    name = "Hide display ads",
    description = "Hides banner, overlay, and in-feed display ads around the app (aRandomHooman).",
    use = true
) {
    try {
        ::DisplayAdResponseParserFingerprint.hookMethod(object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val resultObj = param.result ?: return
                val returnClass = resultObj.javaClass.superclass ?: resultObj.javaClass
                // Look for NoAd singleton or replace result with NoAd instance
                for (field in returnClass.declaredFields) {
                    if (Modifier.isStatic(field.modifiers) && returnClass.isAssignableFrom(field.type)) {
                        field.isAccessible = true
                        val singleton = runCatching { field.get(null) }.getOrNull()
                        if (singleton != null && singleton.javaClass.simpleName.contains("NoAd", ignoreCase = true)) {
                            param.result = singleton
                            return
                        }
                    }
                }
                // Fallback: if class has static fields of its own type, use the NoAd constant (field 'a')
                for (subClass in returnClass.classes) {
                    for (field in subClass.declaredFields) {
                        if (Modifier.isStatic(field.modifiers) && subClass.isAssignableFrom(field.type)) {
                            field.isAccessible = true
                            val singleton = runCatching { field.get(null) }.getOrNull()
                            if (singleton != null) {
                                param.result = singleton
                                return
                            }
                        }
                    }
                }
            }
        })
    } catch (t: Throwable) {
        Logger.printException({ "Failed to hook Hide display ads" }, t)
    }
}

/**
 * De-Vanced: Block video ads (Spoofs client ad eligibility)
 */
val BlockVideoAds = patch(
    name = "Block video ads",
    description = "Blocks client video ad requests in streams and VODs (De-Vanced).",
    use = true
) {
    try {
        ::CheckAdEligibilityFingerprint.hookMethod(object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                // Constructor parameters: (adRequestInfo, shouldRequestAd: Boolean)
                // Force shouldRequestAd = false
                if (param.args.size >= 2 && param.args[1] is Boolean) {
                    param.args[1] = false
                    Logger.printDebug { "Twitch: Block video ads forced shouldRequestAd = false" }
                }
            }
        })
    } catch (t: Throwable) {
        Logger.printException({ "Failed to hook Block video ads" }, t)
    }
}

/**
 * De-Vanced: Block audio ads (Prevents audio ad player invocation)
 */
val BlockAudioAds = patch(
    name = "Block audio ads",
    description = "Blocks audio ads from playing in stream (De-Vanced).",
    use = true
) {
    try {
        ::AudioAdsPlayerPlayFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
    } catch (t: Throwable) {
        Logger.printException({ "Failed to hook Block audio ads" }, t)
    }
}
