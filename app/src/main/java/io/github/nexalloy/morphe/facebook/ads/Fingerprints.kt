package io.github.nexalloy.morphe.facebook.ads

import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall

// Ported from https://github.com/SysAdminDoc/Hushfacebook (GPL-3.0), feed/hook + shared/FeedEdge.

/** Kept GraphQL model class, never renamed by Redex. */
internal const val FEED_UNIT_EDGE = "Lcom/facebook/graphql/model/GraphQLFeedUnitEdge;"
internal const val FEED_UNIT_EDGE_CLASS = "com.facebook.graphql.model.GraphQLFeedUnitEdge"

/** Kept enum. Field names obfuscated, constant names (Enum.name()) survive. */
internal const val FEED_STORY_CATEGORY_CLASS = "com.crossapp.graphql.facebook.enums.GraphQLFeedStoryCategory"

/**
 * FeedUnitCollectionManager.addNewEdgeToCollection: the single funnel every news-feed edge passes.
 * Returning false is handled by the app ("Edge not added to FUC").
 */
internal object AddNewEdgeToCollectionFingerprint : Fingerprint(
    name = "addNewEdgeToCollection",
    returnType = "Z",
    parameters = listOf("Lcom/google/common/collect/ImmutableList\$Builder;", FEED_UNIT_EDGE, "L"),
)

/**
 * FeedUnitCollectionManager$onEdgeSwapped$1.run(): swaps an ad edge (pool_best_ad / hot-swap) over
 * an existing one, bypassing the funnel above.
 */
internal object EdgeSwapRunFingerprint : Fingerprint(
    name = "run",
    returnType = "V",
    parameters = listOf(),
    strings = listOf("Edge swap dropped", "sizeBefore", "sizeAfter"),
    filters = listOf(
        methodCall(parameters = listOf(FEED_UNIT_EDGE, "Ljava/lang/String;"), returnType = "V"),
    ),
)

internal object FeedUnitGetterFingerprint : Fingerprint(
    definingClass = FEED_UNIT_EDGE,
    parameters = listOf(),
    strings = listOf("inflateFeedUnit"),
)

internal object TimelineStoryRenderFingerprint : Fingerprint(
    strings = listOf(
        "sponsored_timeline_stories_test_key",
        "timeline_stories_test_key",
        "TimelineStoryComponent"
    )
)

