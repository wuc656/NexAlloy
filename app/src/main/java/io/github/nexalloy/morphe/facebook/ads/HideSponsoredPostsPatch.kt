package io.github.nexalloy.morphe.facebook.ads

import app.morphe.extension.shared.Logger
import io.github.nexalloy.patch
import java.lang.reflect.Method

private val HIDDEN_CATEGORIES = setOf("SPONSORED", "PROMOTION")

val HideSponsoredPosts = patch(
    name = "Hide sponsored posts",
    description = "Removes sponsored and promoted posts from the news feed.",
) {
    val edgeClass = classLoader.loadClass(FEED_UNIT_EDGE_CLASS)
    val categoryClass = classLoader.loadClass(FEED_STORY_CATEGORY_CLASS)
    // Only zero-arg getter on the edge returning the category enum (cached, never null).
    val categoryGetter: Method = edgeClass.declaredMethods.single {
        it.returnType == categoryClass && it.parameterTypes.isEmpty()
    }.apply { isAccessible = true }

    fun isAd(edge: Any?): Boolean {
        if (edge == null) return false
        val category = runCatching { categoryGetter.invoke(edge) as? Enum<*> }.getOrNull() ?: return false
        return category.name in HIDDEN_CATEGORIES
    }

    AddNewEdgeToCollectionFingerprint.hookMethod {
        before { param ->
            val edge = param.args.firstOrNull { edgeClass.isInstance(it) }
            if (isAd(edge)) {
                Logger.printDebug { "Hide sponsored feed edge" }
                param.result = false
            }
        }
    }

    // Ad hot-swap path never reaches the funnel. Leave the old edge in place.
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
}
