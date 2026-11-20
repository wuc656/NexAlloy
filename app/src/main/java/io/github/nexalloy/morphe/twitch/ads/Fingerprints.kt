package io.github.nexalloy.morphe.twitch.ads

import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.opcodeEnum
import io.github.nexalloy.morphe.strings

/**
 * aRandomHooman: The lambda that builds the LIVE HLS manifest URL handed to the player.
 * It assembles https://usher.ttvnw.net/api/v2/channel/hls/<channel>.m3u8?...
 * Identified by strings "usher.ttvnw.net" and "fast_bread".
 */
val LiveManifestUrlBuilderFingerprint = findMethodDirect {
    findMethod {
        matcher {
            strings("usher.ttvnw.net", "fast_bread")
            name = "invoke"
            paramCount = 2
        }
    }.single()
}

/**
 * aRandomHooman: The parser that turns an ad-edge HTTP response into the app's ad-result union.
 * Identified by strings "failed to parse display ad response: " and "could not parse content type: ".
 */
val DisplayAdResponseParserFingerprint = findMethodDirect {
    findMethod {
        matcher {
            strings(
                "failed to parse display ad response: ",
                "could not parse content type: "
            )
            paramCount = 2
        }
    }.single()
}

/**
 * Extracts the obfuscated NoAd singleton static field from DisplayAdResponseParserFingerprint's HTTP 204 branch.
 */
val displayAdNoAdField = findFieldDirect {
    val parser = DisplayAdResponseParserFingerprint()
    val returnDesc = parser.returnType?.descriptor
    parser.instructions.firstNotNullOf { inst ->
        if (inst.opcodeEnum != Opcode.SGET_OBJECT) return@firstNotNullOf null
        val f = inst.fieldRef ?: return@firstNotNullOf null
        val matchesType = f.typeName == f.declaredClassName &&
            (returnDesc == null || f.declaredClass?.superClass?.descriptor == returnDesc)
        if (matchesType) f else null
    }
}

/**
 * De-Vanced ported to Twitch 31.x:
 * Hook constructor of EligibilityCheckCompleted data class containing "EligibilityCheckCompleted(shouldRequestAd=".
 * Forces shouldRequestAd = false so the client is treated as completely ineligible for video ads.
 */
val CheckAdEligibilityFingerprint = findMethodDirect {
    findClass {
        matcher {
            usingStrings("EligibilityCheckCompleted(shouldRequestAd=")
        }
    }.first().findMethod {
        matcher {
            name = "<init>"
            paramCount = 2
        }
    }.single()
}

/**
 * De-Vanced ported to Twitch 31.x:
 * Method in presenter that broadcasts "tv.twitch.android.media.action.sendAudioAdsContext".
 * Silencing this method blocks audio ads triggers from reaching the media playback service.
 */
val AudioAdsPlayerPlayFingerprint = findMethodDirect {
    findMethod {
        matcher {
            strings("tv.twitch.android.media.action.sendAudioAdsContext")
            paramCount = 0
            returnType = "void"
        }
    }.first()
}
