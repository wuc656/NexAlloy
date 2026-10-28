package io.github.nexalloy.morphe.instagram.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

private val AD_PIVOT_UNITS = setOf("INTENT_AWARE_AD_PIVOT", "STAND_ALONE_MULTI_AD_PIVOT")

val HideInstagramAds = patch(
    name = "HushGram Hide ads",
    description = "Hides sponsored posts, reels and stories without leaving gaps (HushGram).",
) {
    // 1. Guard client-side ad injector
    runCatching {
        ::adInjectorFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
    }

    // 2. Filter server-embedded sponsored posts and ad pivots from parsed feed items (Instagram 450+)
    val mediaClass: Class<*>? = runCatching { classLoader.loadClass("com.instagram.feed.media.Media") }.getOrNull()
    val adDetailsMethod: Method? = runCatching { ::mediaAdDetailsGetter.method.apply { isAccessible = true } }.getOrNull()
    var postField: Field? = null
    var kindField: Field? = null
    var itemResolved = false

    runCatching {
        ::feedItemParserFingerprint.hookMethod(object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val item = param.result ?: return
                if (!itemResolved) {
                    itemResolved = true
                    val itemClass = item.javaClass
                    postField = itemClass.declaredFields.firstOrNull { f ->
                        !Modifier.isStatic(f.modifiers) && f.type == mediaClass
                    }?.apply { isAccessible = true }

                    kindField = itemClass.declaredFields.firstOrNull { f ->
                        !Modifier.isStatic(f.modifiers) && f.type.isEnum &&
                            runCatching {
                                val names = (f.type.enumConstants as? Array<*>)?.mapNotNull { (it as? Enum<*>)?.name }?.toSet().orEmpty()
                                AD_PIVOT_UNITS.all { it in names }
                            }.getOrDefault(false)
                    }?.apply { isAccessible = true }
                }

                // Check if item kind is an ad pivot
                val kind = runCatching { kindField?.get(item) as? Enum<*> }.getOrNull()
                if (kind != null && kind.name in AD_PIVOT_UNITS) {
                    Logger.printDebug { "HushGram: Dropped ${kind.name} item from parsed feed" }
                    param.result = null
                    return
                }

                // Check if item's Media post carries "injected" ad details
                val post = runCatching { postField?.get(item) }.getOrNull()
                if (post != null && adDetailsMethod != null) {
                    val details = runCatching { adDetailsMethod!!.invoke(post) }.getOrNull()
                    if (details != null) {
                        Logger.printDebug { "HushGram: Dropped server-embedded sponsored post from parsed feed" }
                        param.result = null
                    }
                }
            }
        })
    }
}
