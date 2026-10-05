package io.github.nexalloy.morphe.facebook.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch

val BlockAdPrefetch = patch(
    name = "Block background ad prefetch",
    description = "Stops Facebook downloading ads and its ad model in the background.",
) {
    val schedulers = listOf(
        "com.facebook.feed.push.adschannelbackgroundprefetch.FeedAdsChannelBackgroundPrefetchInitializerAppJob",
        "com.facebook.feed.push.adschannelemergingsurfaceprefetch.FeedAdsChannelEmergingSurfacePrefetchInitializerAppJob",
        "com.facebook.video.videohome.prefetching.ads.background.ReelsAdsBackgroundPrefetchAppJob",
        "com.facebook.stories.features.ads.prefetch.StoryViewerAdsPrefetchAppInitializationController",
        "com.facebook.stories.features.ads.prefetch.StoryViewerAdsPrefetchController",
        "com.facebook.addelivery.deliveryvalidation.cachedadsvalidator.NewsFeedAdCacheSyncInitializerAppJob",
        "com.facebook.feed.ads.mlranker.MlRankerAppJob",
    )
    
    schedulers.forEach { className ->
        val clazz = runCatching { classLoader.loadClass(className) }.getOrNull() ?: return@forEach
        
        clazz.declaredMethods.filter { it.returnType.name == "void" }.forEach { method ->
            runCatching {
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (!io.github.nexalloy.morphe.facebook.settings.FacebookSettings.isEnabled(
                                io.github.nexalloy.morphe.facebook.settings.FacebookSettings.KEY_BLOCK_AD_PREFETCH, true
                            )) return
                        Logger.printDebug { "Block ad prefetch: Blocked execution in ${clazz.simpleName}.${method.name}" }
                        param.result = null
                    }
                })
            }
        }
    }
}
