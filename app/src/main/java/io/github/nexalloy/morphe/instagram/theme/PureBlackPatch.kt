package io.github.nexalloy.morphe.instagram.theme

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch
import java.lang.reflect.Modifier

private const val PRISM_BLACK_INT = 0xff0c1014.toInt()
private const val PRISM_BLACK_LONG = 0xff0c1014L
private const val PURE_BLACK_INT = 0xff000000.toInt()
private const val PURE_BLACK_LONG = 0xff000000L

private const val BASE_PRISM_COLORS_CLASS = "com.instagram.compose.core.theme.BasePrismColors"
private const val BASE_PRISM_COLORS_V2_CLASS = "com.instagram.compose.core.theme.BasePrismColorsV2"

val InstagramPureBlackDarkMode = patch(
    name = "Pure black dark mode",
    description = "Instagram's dark mode uses pure black instead of dark gray for OLED screens.",
    use = true
) {
    fun blackenObjectColors(obj: Any?) {
        if (obj == null) return
        for (field in obj.javaClass.declaredFields) {
            if (Modifier.isStatic(field.modifiers)) continue
            field.isAccessible = true
            when (field.type) {
                java.lang.Integer.TYPE -> {
                    val v = runCatching { field.getInt(obj) }.getOrNull()
                    if (v == PRISM_BLACK_INT) {
                        field.setInt(obj, PURE_BLACK_INT)
                        Logger.printDebug { "InstagramPureBlack: Int field ${field.name} set to pure black" }
                    }
                }
                java.lang.Long.TYPE -> {
                    val v = runCatching { field.getLong(obj) }.getOrNull()
                    if (v == PRISM_BLACK_LONG || v == (PRISM_BLACK_LONG shl 32)) {
                        field.setLong(obj, if (v == PRISM_BLACK_LONG) PURE_BLACK_LONG else (PURE_BLACK_LONG shl 32))
                        Logger.printDebug { "InstagramPureBlack: Long field ${field.name} set to pure black" }
                    }
                }
            }
        }
    }

    val hook = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            blackenObjectColors(param.thisObject)
            blackenObjectColors(param.result)
        }
    }

    for (className in listOf(BASE_PRISM_COLORS_CLASS, BASE_PRISM_COLORS_V2_CLASS)) {
        val clazz = runCatching { classLoader.loadClass(className) }.getOrNull() ?: continue
        for (constructor in clazz.declaredConstructors) {
            XposedBridge.hookMethod(constructor, hook)
        }
        for (method in clazz.declaredMethods) {
            if (!Modifier.isAbstract(method.modifiers)) {
                XposedBridge.hookMethod(method, hook)
            }
        }
    }
}
