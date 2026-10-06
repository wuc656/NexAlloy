/*
 * Copyright 2026 HushThreads contributors
 * https://github.com/SysAdminDoc/HushThreads
 * Pure black dark mode for Threads (disabled/commented out)
package io.github.nexalloy.morphe.threads.theme

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.patch
import java.lang.reflect.Modifier

private const val THREADS_DARK_ARGB = 0xff101010L
private const val BLACK_ARGB = 0xff000000L
private const val BLACK_COMPOSE_COLOR = BLACK_ARGB shl 32
private const val DARK_COMPOSE_COLOR = THREADS_DARK_ARGB shl 32

val PureBlackDarkMode = patch(
    name = "Pure black dark mode",
    description = "Changes dark mode backgrounds to pure black (#000000) for OLED power saving (HushThreads).",
    use = true
) {
    fun blackenScheme(schemeObj: Any?) {
        if (schemeObj == null) return
        var count = 0
        for (f in schemeObj.javaClass.declaredFields) {
            if (f.type == java.lang.Long.TYPE && !Modifier.isStatic(f.modifiers)) {
                f.isAccessible = true
                val colorVal = runCatching { f.getLong(schemeObj) }.getOrNull() ?: continue
                if (colorVal == DARK_COMPOSE_COLOR || colorVal == THREADS_DARK_ARGB) {
                    val newColor = if (colorVal == DARK_COMPOSE_COLOR) BLACK_COMPOSE_COLOR else BLACK_ARGB
                    f.setLong(schemeObj, newColor)
                    count++
                }
            }
        }
        if (count > 0) {
            Logger.printDebug { "PureBlack: Modified $count colors in scheme ${schemeObj.javaClass.name} to pure black" }
        }
    }

    fun modifyStaticHolder() {
        val themeMethod = runCatching { BdsThemeFingerprint.method }.getOrNull() ?: return
        val clz = themeMethod.declaringClass
        for (field in clz.declaredFields) {
            if (Modifier.isStatic(field.modifiers)) {
                field.isAccessible = true
                val obj = runCatching { field.get(null) }.getOrNull()
                blackenScheme(obj)
            }
        }
    }

    fun modifyTargetClass(loader: ClassLoader) {
        // Direct target class X.9tj contains static A02 (Dark ColorScheme of type X.9tk)
        val targetClassNames = listOf("X.9tj")
        for (name in targetClassNames) {
            runCatching {
                val clz = loader.loadClass(name)
                for (field in clz.declaredFields) {
                    if (Modifier.isStatic(field.modifiers)) {
                        field.isAccessible = true
                        val obj = runCatching { field.get(null) }.getOrNull()
                        blackenScheme(obj)
                    }
                }
            }
        }
    }

    // Hook BdsTheme fingerprint method
    BdsThemeFingerprint.hookMethod(object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val loader = param.thisObject?.javaClass?.classLoader 
                ?: BdsThemeFingerprint.method?.declaringClass?.classLoader 
                ?: return
            modifyTargetClass(loader)
            modifyStaticHolder()
        }

        override fun afterHookedMethod(param: MethodHookParam) {
            val loader = param.thisObject?.javaClass?.classLoader 
                ?: BdsThemeFingerprint.method?.declaringClass?.classLoader
            if (loader != null) {
                modifyTargetClass(loader)
            }
            modifyStaticHolder()
            blackenScheme(param.thisObject)
            blackenScheme(param.result)
            val args = param.args ?: return
            for (arg in args) {
                blackenScheme(arg)
            }
        }
    })

    // Also hook X.9tk constructors if present
    runCatching {
        val loader = BdsThemeFingerprint.method?.declaringClass?.classLoader
        if (loader != null) {
            modifyTargetClass(loader)
            val schemeClass = runCatching { loader.loadClass("X.9tk") }.getOrNull()
            if (schemeClass != null) {
                for (ctor in schemeClass.declaredConstructors) {
                    de.robv.android.xposed.XposedBridge.hookMethod(ctor, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            blackenScheme(param.thisObject)
                        }
                    })
                }
                Logger.printDebug { "PureBlack: Hooked constructors of X.9tk" }
            }
        }
    }
}
*/

