package io.github.nexalloy.morphe.messenger.ads

import io.github.nexalloy.morphe.Fingerprint

internal const val IMMUTABLE_LIST_CLASS = "com.google.common.collect.ImmutableList"
internal const val INBOX_ADS_ITEM_CLASS = "com.facebook.messaging.business.inboxads.common.InboxAdsItem"

/**
 * Messenger ItemListProcessor ads filter method fingerprint.
 * Finds the method processing inbox items: returns ImmutableList, takes (..., ImmutableList, String),
 * containing strings "messaging.inbox.itemlistprocessor.ItemListProcessorInterfaceSpec", "processItems", "new_friend_bump_threads".
 */
internal object InboxAdsProcessorFingerprint : Fingerprint(
    returnType = "L$IMMUTABLE_LIST_CLASS;",
    parameters = listOf("L", "L$IMMUTABLE_LIST_CLASS;", "Ljava/lang/String;"),
    strings = listOf(
        "messaging.inbox.itemlistprocessor.ItemListProcessorInterfaceSpec",
        "processItems",
        "new_friend_bump_threads"
    )
)
