package io.github.nexalloy.morphe.threads.theme

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.patch
import java.lang.reflect.Modifier

private const val THREADS_DARK_ARGB = 0xff101010L
private const val BLACK_ARGB = 0xff000000L
private const val BLACK_COMPOSE_COLOR = BLACK_ARGB shl 32

val PureBlackDarkMode = patch(
    name = "Pure black dark mode",
    description = "Changes dark mode backgrounds to pure black (#000000) for OLED power saving.",
    use = true
) {
    BdsThemeFingerprint.hookMethod(object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val themeObject = param.thisObject ?: return
            for (field in themeObject.javaClass.declaredFields) {
                if (Modifier.isStatic(field.modifiers)) continue
                field.isAccessible = true
                val value = runCatching { field.get(themeObject) }.getOrNull() ?: continue
                
                for (f in value.javaClass.declaredFields) {
                    if (f.type == java.lang.Long.TYPE) {
                        f.isAccessible = true
                        val colorVal = runCatching { f.getLong(value) }.getOrNull() ?: continue
                        if (colorVal == (THREADS_DARK_ARGB shl 32)) {
                            f.setLong(value, BLACK_COMPOSE_COLOR)
                            Logger.printDebug { "PureBlack: Modified color field ${f.name} to pure black" }
                        }
                    }
                }
            }
        }
    })
}
