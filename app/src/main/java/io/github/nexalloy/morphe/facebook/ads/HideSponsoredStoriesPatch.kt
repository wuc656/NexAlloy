package io.github.nexalloy.morphe.facebook.ads

import app.morphe.extension.shared.Logger
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.patch

internal object StoryHandlingClashFingerprint : Fingerprint(returnType = "Lcom/google/common/collect/ImmutableList;", strings = listOf("handling_inorganic_clash"))
internal object StoryUninsertedAdsFingerprint : Fingerprint(returnType = "Lcom/google/common/collect/ImmutableList;", strings = listOf("uninsertedMainAdsQueue"))
internal object StoryAdBucketFingerprint : Fingerprint(returnType = "Lcom/google/common/collect/ImmutableList;", strings = listOf("AdPaginatingBucketStaticInsertionDataSource.getBuckets"))
internal object StoryMidCardBucketFingerprint : Fingerprint(returnType = "Lcom/google/common/collect/ImmutableList;", strings = listOf("StoryViewerMidCardDataSource.getBuckets"))

val HideSponsoredStories = patch(
    name = "Hide sponsored stories",
    description = "Removes ad cards from the story viewer, so swiping through stories only shows stories people posted.",
) {
    val fingerprints = listOf(
        StoryHandlingClashFingerprint,
        StoryUninsertedAdsFingerprint,
        StoryAdBucketFingerprint,
        StoryMidCardBucketFingerprint
    )

    fingerprints.forEach { fp ->
        fp.hookMethod {
            before { param ->
                if (!io.github.nexalloy.morphe.facebook.settings.FacebookSettings.isEnabled(
                        io.github.nexalloy.morphe.facebook.settings.FacebookSettings.KEY_HIDE_SPONSORED_STORIES, true
                    )) return@before
                val method = param.method as java.lang.reflect.Method
                val listParamIndex = method.parameterTypes.indexOfLast { it.name == "com.google.common.collect.ImmutableList" }
                if (listParamIndex >= 0) {
                    Logger.printDebug { "Hide sponsored stories: Bypassed ad bucket inserter" }
                    param.result = param.args[listParamIndex]
                }
            }
        }
    }
}

