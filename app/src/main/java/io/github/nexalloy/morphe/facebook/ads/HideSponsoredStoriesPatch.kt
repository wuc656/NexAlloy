package io.github.nexalloy.morphe.facebook.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.strings
import io.github.nexalloy.patch

private const val IMMUTABLE_LIST_CLASS_NAME = "com.google.common.collect.ImmutableList"

internal object StoryHandlingClashFingerprint : Fingerprint(returnType = "Lcom/google/common/collect/ImmutableList;", strings = listOf("handling_inorganic_clash"))
internal object StoryAdBucketFingerprint : Fingerprint(returnType = "Lcom/google/common/collect/ImmutableList;", strings = listOf("AdPaginatingBucketStaticInsertionDataSource.getBuckets"))
internal object StoryMidCardBucketFingerprint : Fingerprint(returnType = "Lcom/google/common/collect/ImmutableList;", strings = listOf("StoryViewerMidCardDataSource.getBuckets"))

/**
 * Matches AdBucketDataSourceUtil's bucket source directly (<=587) or its ImmutableList caller (588+ where uninsertedMainAdsQueue moved to a List helper).
 */
val storyUninsertedAdsSourceMethod = findMethodDirect {
    findMethod {
        matcher {
            returnType = IMMUTABLE_LIST_CLASS_NAME
            strings("uninsertedMainAdsQueue")
        }
    }.firstOrNull() ?: run {
        val helpers = findMethod {
            matcher {
                returnType = "java.util.List"
                strings("uninsertedMainAdsQueue")
            }
        }
        helpers.firstNotNullOf { helper ->
            findMethod {
                matcher {
                    returnType = IMMUTABLE_LIST_CLASS_NAME
                    addInvoke {
                        declaredClass(helper.declaredClassName)
                        name = helper.name
                    }
                }
            }.firstOrNull { it.paramTypeNames.lastOrNull() == IMMUTABLE_LIST_CLASS_NAME }
        }
    }
}

val HideSponsoredStories = patch(
    name = "Hide sponsored stories",
    description = "Removes ad cards from the story viewer, so swiping through stories only shows stories people posted.",
) {
    val passThroughHook = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            if (!FacebookSettings.isEnabled(FacebookSettings.KEY_HIDE_SPONSORED_STORIES, true)) return
            val method = param.method as java.lang.reflect.Method
            val listParamIndex = method.parameterTypes.indexOfLast { it.name == IMMUTABLE_LIST_CLASS_NAME }
            if (listParamIndex >= 0) {
                Logger.printDebug { "Hide sponsored stories: Bypassed ad bucket inserter" }
                param.result = param.args[listParamIndex]
            }
        }
    }

    listOf(
        StoryHandlingClashFingerprint,
        StoryAdBucketFingerprint,
        StoryMidCardBucketFingerprint
    ).forEach { fp ->
        runCatching { fp.hookMethod(passThroughHook) }
    }

    runCatching {
        ::storyUninsertedAdsSourceMethod.hookMethod(passThroughHook)
    }
}
