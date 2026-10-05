package io.github.nexalloy.morphe.facebook.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch
import java.lang.reflect.Field
import java.lang.reflect.Method

private const val SPONSORED_TEST_KEY = "sponsored_timeline_stories_test_key"
private const val COMPONENT_NAME = "TimelineStoryComponent"
private const val GRAPHQL_STORY_CLASS = "com.facebook.graphql.model.GraphQLStory"

internal object TimelineStoryComponentFingerprint : Fingerprint(
    strings = listOf(SPONSORED_TEST_KEY, COMPONENT_NAME)
)

val HideSponsoredProfilePosts = patch(
    name = "Hide sponsored profile posts",
    description = "Removes the ads between the posts on someone's profile or a Page.",
) {
    var unitField: Field? = null
    var sponsoredDataAccessor: Method? = null
    var initialized = false

    TimelineStoryComponentFingerprint.hookMethod(object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            if (!FacebookSettings.isEnabled(FacebookSettings.KEY_HIDE_SPONSORED_PROFILE_POSTS, true)) return

            val componentInstance = param.thisObject ?: return

            if (!initialized) {
                initialized = true
                val clazz = componentInstance.javaClass
                val storyClass = runCatching { classLoader.loadClass(GRAPHQL_STORY_CLASS) }.getOrNull()
                if (storyClass != null) {
                    val storyInterfaces = mutableSetOf<Class<*>>()
                    var curr: Class<*>? = storyClass
                    while (curr != null) {
                        storyInterfaces.addAll(curr.interfaces)
                        curr = curr.superclass
                    }

                    unitField = clazz.declaredFields.firstOrNull { f ->
                        storyInterfaces.any { iface -> iface.isAssignableFrom(f.type) }
                    }?.apply { isAccessible = true }

                    sponsoredDataAccessor = storyClass.declaredMethods.firstOrNull { m ->
                        m.parameterTypes.isEmpty() && (m.returnType.name.endsWith("SponsoredData") || m.returnType.simpleName == "GraphQLSponsoredData")
                    }?.apply { isAccessible = true }
                }
            }

            val field = unitField ?: return
            val unit = field.get(componentInstance) ?: return

            if (GRAPHQL_STORY_CLASS == unit.javaClass.name || unit.javaClass.superclass?.name == GRAPHQL_STORY_CLASS) {
                val sponsoredData = runCatching {
                    sponsoredDataAccessor?.invoke(unit)
                }.getOrNull()

                if (sponsoredData != null) {
                    Logger.printDebug { "HideSponsoredProfilePosts: Blocked sponsored profile post" }
                    param.result = null
                }
            }
        }
    })
}
