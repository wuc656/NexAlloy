package io.github.nexalloy.morphe.messenger.ads

import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.opcodeEnum
import io.github.nexalloy.morphe.strings
import java.lang.reflect.Modifier

internal const val IMMUTABLE_LIST_CLASS = "com.google.common.collect.ImmutableList"
internal const val IMMUTABLE_LIST_DESC = "Lcom/google/common/collect/ImmutableList;"
internal const val INBOX_ADS_ITEM_CLASS = "com.facebook.messaging.business.inboxads.common.InboxAdsItem"

/**
 * Messenger ItemListProcessor ads filter method fingerprint.
 * Finds the method processing inbox items: returns ImmutableList, takes (..., ImmutableList, String),
 * containing strings "messaging.inbox.itemlistprocessor.ItemListProcessorInterfaceSpec", "processItems", "new_friend_bump_threads".
 */
internal object InboxAdsProcessorFingerprint : Fingerprint(
    returnType = IMMUTABLE_LIST_DESC,
    parameters = listOf("L", IMMUTABLE_LIST_DESC, "Ljava/lang/String;"),
    strings = listOf(
        "messaging.inbox.itemlistprocessor.ItemListProcessorInterfaceSpec",
        "processItems",
        "new_friend_bump_threads"
    )
)

/**
 * Legacy Messenger (<=585) inbox ads item supplier.
 */
internal object LoadInboxAdsFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/messaging/business/inboxads/plugins/inboxads/itemsupplier/InboxAdsItemSupplierImplementation;",
    returnType = "V",
    strings = listOf("ads_load_begin", "inbox_ads_fetch_start")
)

/**
 * Click-to-message ad entry event logger ("ad_id", "page_id").
 */
internal object AdEntryEventFingerprint : Fingerprint(
    isStatic = true,
    returnType = "V",
    parameters = listOf(
        "L",
        "Lcom/facebook/auth/usersession/FbUserSession;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;"
    ),
    strings = listOf("ad_id", "page_id")
)

/**
 * InboxCustomizedImpressionTracker ("inbox2_vr") visibility callback.
 */
internal object InboxVisibilityEventFingerprint : Fingerprint(
    isStatic = false,
    returnType = "V",
    parameters = listOf("L", "Z"),
    strings = listOf("InboxCustomizedImpressionTracker", "inbox2_vr")
)

/**
 * Logger submit call inside InboxVisibilityEventFingerprint after "inbox2_vr" event construction.
 */
val inboxVisibilityLoggerMethod = findMethodDirect {
    val trackerMethod = InboxVisibilityEventFingerprint.run()
    val code = trackerMethod.instructions.toList()
    val eventIndex = code.indexOfFirst { it.string == "inbox2_vr" }
    require(eventIndex >= 0) { "inbox2_vr string instruction not found in $trackerMethod" }

    code.drop(eventIndex + 1).firstNotNullOf { inst ->
        if (inst.opcodeEnum != Opcode.INVOKE_VIRTUAL && inst.opcodeEnum != Opcode.INVOKE_VIRTUAL_RANGE) {
            return@firstNotNullOf null
        }
        inst.methodRef?.takeIf { ref ->
            ref.paramTypeNames.size == 1 && ref.returnTypeName == "void"
        }
    }
}

/**
 * LatStatusJob ad attribution upload worker ("AttributionIdUpdate" + IAdvertisingIdService).
 */
val attributionUploadMethod = findMethodDirect {
    findMethod {
        matcher {
            returnType = "void"
            strings(
                "Failure while creating and sending attribution state through AttributionIdUpdate.",
                "com.google.android.gms.ads.identifier.internal.IAdvertisingIdService"
            )
        }
    }.first { m ->
        !Modifier.isAbstract(m.modifiers) &&
            m.paramTypeNames.any { it == "com.facebook.auth.usersession.FbUserSession" }
    }
}
