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
    runCatching {
        GetSponsoredMessagesFingerprint.hookMethod(XC_MethodReplacement.returnConstant(null))
    }

    // 2. Video player ads
    runCatching {
        VideoAdsLoadFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
    }

    // 3. Search sponsored peers: block ConnectionsManager.sendRequest when payload is TL_contacts_getSponsoredPeers
    runCatching {
        val connectionsManager = classLoader.loadClass("org.telegram.tgnet.ConnectionsManager")
        for (method in connectionsManager.declaredMethods) {
            if (method.name == "sendRequest" && method.parameterTypes.isNotEmpty()) {
                de.robv.android.xposed.XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val req = param.args.firstOrNull() ?: return
                        if (req.javaClass.name == "org.telegram.tgnet.TLRPC\$TL_contacts_getSponsoredPeers") {
                            Logger.printDebug { "Telegram: Blocked TL_contacts_getSponsoredPeers sendRequest" }
                            param.result = 0
                        }
                    }
                })
            }
        }
    }
}
