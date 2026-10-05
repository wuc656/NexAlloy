package io.github.nexalloy.morphe.telegram.ads

import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.newInstance

internal const val GET_SPONSORED_MESSAGES = "Lorg/telegram/tgnet/TLRPC\$TL_messages_getSponsoredMessages;"
internal const val MESSAGES_CONTROLLER = "Lorg/telegram/messenger/MessagesController;"
internal const val GET_SPONSORED_PEERS = "Lorg/telegram/tgnet/TLRPC\$TL_contacts_getSponsoredPeers;"

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
    name = "load",
    returnType = "V",
    parameters = listOf()
)

/**
 * Global search query method requesting sponsored peers
 */
internal object SearchSponsoredPeersFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(newInstance(GET_SPONSORED_PEERS))
)
