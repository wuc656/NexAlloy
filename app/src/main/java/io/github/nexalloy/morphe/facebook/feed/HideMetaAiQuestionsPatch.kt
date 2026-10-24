package io.github.nexalloy.morphe.facebook.feed

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch

private const val GEN_AI_PILL_PLUGIN_CLASS = "com.facebook.feed.plugins.attachments.deepdivepill.impl.genai.GenAiDeepDivePillPlugin"
private const val RELATED_POSTS_PLUGIN_CLASS = "com.facebook.feed.plugins.belowufifooter.impl.inlinerelatedposts.InlineRelatedPostsPlugin"

/**
 * Hide Meta AI questions and Related posts under posts:
 * Hooks GenAiDeepDivePillPlugin and InlineRelatedPostsPlugin methods to prevent creating or showing the rows.
 */
val HideMetaAiQuestions = patch(
    name = "Hide Meta AI questions under posts",
    description = "Removes the row of Meta AI questions and AI related posts Facebook puts under some posts.",
) {
    val pluginClass = runCatching { classLoader.loadClass(GEN_AI_PILL_PLUGIN_CLASS) }.getOrNull()
    if (pluginClass == null) {
        Logger.printDebug { "HideMetaAiQuestions: GenAiDeepDivePillPlugin class not found" }
    } else {
        pluginClass.declaredMethods.forEach { method ->
            if (method.returnType == java.lang.Boolean.TYPE || method.returnType == java.lang.Boolean::class.java) {
                runCatching {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (FacebookSettings.isEnabled(FacebookSettings.KEY_HIDE_META_AI_QUESTIONS, true)) {
                                param.result = false
                            }
                        }
                    })
                }
            }
        }
    }

    val relatedPostsClass = runCatching { classLoader.loadClass(RELATED_POSTS_PLUGIN_CLASS) }.getOrNull()
    if (relatedPostsClass == null) {
        Logger.printDebug { "HideMetaAiQuestions: InlineRelatedPostsPlugin class not found" }
    } else {
        relatedPostsClass.declaredMethods.forEach { method ->
            if (method.returnType == java.lang.Boolean.TYPE || method.returnType == java.lang.Boolean::class.java) {
                runCatching {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (FacebookSettings.isEnabled(FacebookSettings.KEY_HIDE_SUGGESTED_POSTS, true)) {
                                param.result = false
                            }
                        }
                    })
                }
            }
        }
    }
}
