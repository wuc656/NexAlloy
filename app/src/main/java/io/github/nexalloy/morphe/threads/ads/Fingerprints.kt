package io.github.nexalloy.morphe.threads.ads

import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.methodCall

internal const val FEED_CACHE_CLASS = "com.instagram.barcelona.feed.data.cache.BarcelonaFeedCache"
internal const val MEDIA_CLASS = "com.instagram.feed.media.Media"

internal object FeedPageMergeFingerprint : Fingerprint(
    definingClass = "L$FEED_CACHE_CLASS;",
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
