package io.github.nexalloy.morphe.facebook.ads

import app.morphe.extension.shared.Logger
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.patch

internal object FloatingCardReaderFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/feedback/comments/plugins/indicatorpill/organicaffiliatefloatingcta/OrganicAffiliateFloatingCtaPlugin;"
)

val HideAffiliateLinks = patch(
    name = "Hide affiliate product links",
    description = "Removes the product cards of affiliate shop links from reels, feed posts and the comment sheet.",
) {
    FloatingCardReaderFingerprint.hookMethod {
        before { param ->
            val method = param.method as java.lang.reflect.Method
            // Only hook static readers
            if (java.lang.reflect.Modifier.isStatic(method.modifiers) && 
                method.parameterTypes.size == 1 &&
                !method.returnType.isPrimitive) {
                if (io.github.nexalloy.morphe.facebook.settings.FacebookSettings.isEnabled(
                        io.github.nexalloy.morphe.facebook.settings.FacebookSettings.KEY_HIDE_AFFILIATE_LINKS, true
                    )) {
                    Logger.printDebug { "Hide affiliate links: Blocked floating card" }
                    param.result = null
                }
            }
        }
    }
}
