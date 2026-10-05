package io.github.nexalloy.morphe.facebook.ads

import app.morphe.extension.shared.Logger
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.patch

val HideSponsoredStories = patch(
    name = "Hide sponsored stories",
    description = "Removes ad cards from the story viewer, so swiping through stories only shows stories people posted.",
) {
    val IMMUTABLE_LIST = "Lcom/google/common/collect/ImmutableList;"

    val fingerprints = listOf(
        Fingerprint(returnType = IMMUTABLE_LIST, strings = listOf("handling_inorganic_clash")),
        Fingerprint(returnType = IMMUTABLE_LIST, strings = listOf("uninsertedMainAdsQueue")),
        Fingerprint(returnType = IMMUTABLE_LIST, strings = listOf("AdPaginatingBucketStaticInsertionDataSource.getBuckets")),
        Fingerprint(returnType = IMMUTABLE_LIST, strings = listOf("StoryViewerMidCardDataSource.getBuckets"))
    )

    fingerprints.forEach { fp ->
        fp.hookMethod {
            before { param ->
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

