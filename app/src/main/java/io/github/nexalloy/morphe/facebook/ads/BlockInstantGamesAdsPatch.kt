package io.github.nexalloy.morphe.facebook.ads

import app.morphe.extension.shared.Logger
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.strings
import io.github.nexalloy.patch
import org.json.JSONObject
import java.lang.reflect.Method

private const val REJECT_LOG =
    "Unexpected exception while constructing JSONObject to be dispatched to Javascript: rejectPromise"
private const val NO_AD_MESSAGE = "No ad is available."

private val AD_MESSAGE_CODES = mapOf(
    "getinterstitialadasync" to "CLIENT_UNSUPPORTED_OPERATION",
    "getrewardedvideoasync" to "CLIENT_UNSUPPORTED_OPERATION",
    "loadadasync" to "ADS_NO_FILL",
    "loadbanneradasync" to "ADS_NO_FILL",
    "showadasync" to "ADS_NOT_LOADED"
)

/**
 * QuicksilverAndroid JavaScript bridge postMessage(String, String)V.
 */
val instantGamesPostMessageMethod = findMethodDirect {
    val bridgeClass = findClass {
        matcher {
            addMethod {
                strings("getinterstitialadasync")
            }
        }
    }.single()

    bridgeClass.findMethod {
        matcher {
            name = "postMessage"
            returnType = "void"
            paramTypes("java.lang.String", "java.lang.String")
        }
    }.single()
}

/**
 * Bridge's 3-arg promise rejection method invoking the rejectPromise JSON helper.
 */
val instantGamesRejectPromiseMethod = findMethodDirect {
    val bridgeClass = findClass {
        matcher {
            addMethod {
                strings("getinterstitialadasync")
            }
        }
    }.single()

    val rejectHelper = findMethod {
        matcher {
            returnType = "void"
            paramTypes("java.lang.String", "java.lang.String", "java.lang.String")
            strings(REJECT_LOG)
        }
    }.first()

    bridgeClass.findMethod {
        matcher {
            returnType = "void"
            paramTypes("java.lang.String", "java.lang.String", "java.lang.String")
            addInvoke {
                declaredClass(rejectHelper.declaredClassName)
                name = rejectHelper.name
            }
        }
    }.first()
}

val BlockInstantGamesAds = patch(
    name = "Block Instant Games ads",
    description = "Blocks interstitial, rewarded, and banner ad requests inside Facebook Instant Games WebViews.",
) {
    val rejectPromise: Method? = runCatching {
        ::instantGamesRejectPromiseMethod.method.apply { isAccessible = true }
    }.getOrNull()

    runCatching {
        ::instantGamesPostMessageMethod.hookMethod {
            before { param ->
                if (!FacebookSettings.isEnabled(FacebookSettings.KEY_BLOCK_GAME_ADS, true)) return@before
                val message = param.args?.getOrNull(0) as? String ?: return@before
                val json = runCatching { JSONObject(message) }.getOrNull() ?: return@before
                val type = json.optString("type", "").lowercase()
                val code = AD_MESSAGE_CODES[type] ?: return@before
                val content = json.optJSONObject("content") ?: return@before
                val promiseId = content.optString("promiseID", "")
                if (promiseId.isEmpty()) return@before

                val bridge = param.thisObject ?: return@before
                val reject = rejectPromise ?: return@before
                runCatching {
                    reject.invoke(bridge, promiseId, code, NO_AD_MESSAGE)
                    Logger.printDebug { "BlockInstantGamesAds: Rejected $type ($code)" }
                    param.result = null
                }
            }
        }
    }
}
