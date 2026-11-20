package io.github.nexalloy.morphe.telegram.ads

import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.newInstance

internal const val GET_SPONSORED_MESSAGES = "Lorg/telegram/tgnet/TLRPC\$TL_messages_getSponsoredMessages;"
internal const val MESSAGES_CONTROLLER = "Lorg/telegram/messenger/MessagesController;"
internal const val MESSAGES_CONTROLLER_CLASS = "org.telegram.messenger.MessagesController"

/**
 * MessagesController.getSponsoredMessages(long)
 */
internal object GetSponsoredMessagesFingerprint : Fingerprint(
    definingClass = MESSAGES_CONTROLLER,
    returnType = "Lorg/telegram/messenger/MessagesController\$SponsoredMessagesInfo;",
    parameters = listOf("J"),
    filters = listOf(newInstance(GET_SPONSORED_MESSAGES))
)

/**
 * VideoAds.load()
 */
internal object VideoAdsLoadFingerprint : Fingerprint(
    definingClass = "Lorg/telegram/messenger/video/VideoAds;",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(newInstance(GET_SPONSORED_MESSAGES))
)

/**
 * MessagesController.sortDialogs(...) method handling promoDialog and isLeftPromoChannel reinsertion.
 */
val sortDialogsMethod = findMethodDirect {
    findMethod {
        matcher {
            declaredClass(MESSAGES_CONTROLLER_CLASS)
            returnType = "void"
            paramCount = 1
            addUsingField {
                declaredClass(MESSAGES_CONTROLLER_CLASS)
                name = "promoDialog"
            }
            addUsingField {
                declaredClass(MESSAGES_CONTROLLER_CLASS)
                name = "isLeftPromoChannel"
            }
            addInvoke {
                declaredClass("java.util.ArrayList")
                name = "add"
                paramTypes("int", "java.lang.Object")
            }
        }
    }.first()
}
