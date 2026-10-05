package io.github.nexalloy.morphe.facebook.ads

import app.morphe.extension.shared.Logger
import io.github.nexalloy.patch
import java.lang.reflect.Method

private val HIDDEN_CATEGORIES = setOf("SPONSORED", "PROMOTION")

private val SUGGESTED_UNITS = setOf(
    "GraphQLPagesYouMayLikeFeedUnit",
    "GraphQLPaginatedPagesYouMayLikeFeedUnit",
    "GraphQLCreativePagesYouMayLikeFeedUnit",
    "GraphQLPYMLWithLargeImageFeedUnit",
    "GraphQLPagesYouMayFollowFeedUnit",
    "GraphQLPagesYouMayAdvertiseFeedUnit",
    "GraphQLPymgfFeedUnit",
    "GraphQLQuickPromotionFeedUnit",
    "GraphQLQuickPromotionNativeTemplateFeedUnit",
    "GraphQLEndOfFeedUpsellCustomNTFeedUnit",
    "GraphQLExploreFeedUpsellNTUnit",
    "GraphQLGreetingCardPromotionFeedUnit",
    "GraphQLStoryGallerySurveyFeedUnit",
    "GraphQLBusinessPageReviewFeedUnit",
    "GraphQLHoldoutAdFeedUnit"
)

private val SUGGESTED_TYPE_NAMES = setOf(
    "PaginatedPeopleYouMayKnowFeedUnit",
    "GroupsYouShouldJoinFeedUnit",
    "DiscoverFeedUnit",
    "ShowreelNativeFeedUnit",
    "ShortFormVideoAttachmentFeedUnit"
)

val HideSponsoredPosts = patch(
    name = "Hide sponsored and suggested posts",
    description = "Removes sponsored, promoted, and suggested posts from the news feed.",
) {
    val edgeClass = classLoader.loadClass(FEED_UNIT_EDGE_CLASS)
    val categoryClass = classLoader.loadClass(FEED_STORY_CATEGORY_CLASS)
    val categoryGetter: Method = edgeClass.declaredMethods.single {
        it.returnType == categoryClass && it.parameterTypes.isEmpty()
    }.apply { isAccessible = true }

    var cachedFeedUnitGetter: Method? = null

    fun isSuggestedUnit(obj: Any?): Boolean {
        if (obj == null) return false
        val simpleName = obj.javaClass.simpleName
        if (simpleName in SUGGESTED_UNITS) {
            return io.github.nexalloy.morphe.facebook.settings.FacebookSettings.isEnabled(
                io.github.nexalloy.morphe.facebook.settings.FacebookSettings.KEY_HIDE_SUGGESTED_POSTS, true
            )
        }
        
        val typeNameMethod = runCatching { obj.javaClass.getMethod("getTypeName") }.getOrNull()
        if (typeNameMethod != null) {
            val typeName = runCatching { typeNameMethod.invoke(obj) as? String }.getOrNull()
            if (typeName in SUGGESTED_TYPE_NAMES) {
                if (typeName == "ShowreelNativeFeedUnit" || typeName == "ShortFormVideoAttachmentFeedUnit") {
                    return io.github.nexalloy.morphe.facebook.settings.FacebookSettings.isEnabled(
                        io.github.nexalloy.morphe.facebook.settings.FacebookSettings.KEY_HIDE_REELS_IN_FEED, true
                    )
                }
                return io.github.nexalloy.morphe.facebook.settings.FacebookSettings.isEnabled(
                    io.github.nexalloy.morphe.facebook.settings.FacebookSettings.KEY_HIDE_SUGGESTED_POSTS, true
                )
            }
        }
        return false
    }

    fun isAd(edge: Any?): Boolean {
        if (edge == null) return false
        val category = runCatching { categoryGetter.invoke(edge) as? Enum<*> }.getOrNull()
        if (category?.name in HIDDEN_CATEGORIES) {
            return io.github.nexalloy.morphe.facebook.settings.FacebookSettings.isEnabled(
                io.github.nexalloy.morphe.facebook.settings.FacebookSettings.KEY_HIDE_SPONSORED_POSTS, true
            )
        }
        
        if (cachedFeedUnitGetter != null) {
            val feedUnit = runCatching { cachedFeedUnitGetter!!.invoke(edge) }.getOrNull()
            return isSuggestedUnit(feedUnit)
        }

        // Search for the feed unit getter
        for (method in edgeClass.declaredMethods) {
            if (method.parameterTypes.isNotEmpty() || method.returnType.isPrimitive || method.returnType == String::class.java) continue
            method.isAccessible = true
            val obj = runCatching { method.invoke(edge) }.getOrNull() ?: continue
            
            // Heuristic: If it has a getTypeName method, it is likely the FeedUnit
            if (runCatching { obj.javaClass.getMethod("getTypeName") }.isSuccess) {
                cachedFeedUnitGetter = method
                return isSuggestedUnit(obj)
            }
        }
        
        return false
    }

    AddNewEdgeToCollectionFingerprint.hookMethod {
        before { param ->
            val edge = param.args.firstOrNull { edgeClass.isInstance(it) }
            if (isAd(edge)) {
                Logger.printDebug { "Hide sponsored or suggested feed edge" }
                param.result = false
            }
        }
    }

    EdgeSwapRunFingerprint.hookMethod {
        before { param ->
            val runnable = param.thisObject
            val edge = runnable.javaClass.declaredFields.mapNotNull {
                it.isAccessible = true
                it.get(runnable)
            }.firstOrNull { edgeClass.isInstance(it) }

            if (isAd(edge)) {
                Logger.printDebug { "Block sponsored edge swap" }
                param.result = null // Skip the run() execution
            }
        }
    }

    TimelineStoryRenderFingerprint.hookMethod {
        before { param ->
            val component = param.thisObject ?: return@before
            for (field in component.javaClass.declaredFields) {
                field.isAccessible = true
                val value = runCatching { field.get(component) }.getOrNull() ?: continue
                if (isAd(value)) {
                    Logger.printDebug { "Hide sponsored profile posts: Blocked ad on timeline" }
                    param.result = null
                    break
                }
            }
        }
    }
}

