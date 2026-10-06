/*
 * Copyright 2026 HushThreads contributors
 * https://github.com/SysAdminDoc/HushThreads
 */
package io.github.nexalloy.morphe.threads.ads

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall

internal const val FEED_CACHE = "Lcom/instagram/barcelona/feed/data/cache/BarcelonaFeedCache;"
internal const val MEDIA = "Lcom/instagram/feed/media/Media;"
internal val INJECTED_FIELD = "injected".hashCode()

/**
 * BarcelonaFeedCache.addAndSaveItemsFromFeedFetchSuccess:
 * Merges a fetched feed page into the feed cache.
 * The 5th parameter (index 4) is the List of items.
 */
internal object FeedPageMergeFingerprint : Fingerprint(
    definingClass = FEED_CACHE,
    returnType = "Ljava/lang/Object;",
    parameters = listOf(
        "L", "Ljava/lang/Integer;", "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/util/List;",
        "L", "Lkotlin/jvm/functions/Function3;", "Z"
    ),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/instagram/barcelona/feed/data/cache/BarcelonaFeedCache\$addAndSaveItemsFromFeedFetchSuccess\$2\$1;",
            name = "<init>"
        )
    )
)

/**
 * InjectedAdCheckFingerprint:
 * Threads' check of whether a post carries the "injected" block the server puts on an ad.
 * Static boolean method taking the post's fragment.
 * Uses 0x8669a9b0 and INJECTED_FIELD ("injected".hashCode()).
 */
internal object InjectedAdCheckFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("L"),
    filters = listOf(
        literal(0x8669a9b0.toInt()),
        literal(INJECTED_FIELD)
    )
)
