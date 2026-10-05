package io.github.nexalloy.morphe.telegram.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val HideTelegramAds = patch(
    name = "Hide ads",
    description = "Hides sponsored messages in channels, ads in video player, and sponsored accounts in global search.",
    use = true
) {
    // 1. Channel sponsored messages
    GetSponsoredMessagesFingerprint.hookMethod(XC_MethodReplacement.returnConstant(null))

    // 2. Video player ads
    VideoAdsLoadFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)

    // 3. Search sponsored peers (optional: hook if present in build)
    runCatching {
        SearchSponsoredPeersFingerprint.hookMethod(object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                Logger.printDebug { "Telegram: Blocked search sponsored peers request" }
                param.result = null
            }
        })
    }
}
