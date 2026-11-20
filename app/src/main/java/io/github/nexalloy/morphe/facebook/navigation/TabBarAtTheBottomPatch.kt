package io.github.nexalloy.morphe.facebook.navigation

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.opcodeEnum
import io.github.nexalloy.morphe.strings
import io.github.nexalloy.patch
import java.lang.reflect.Modifier

private const val OVERRIDE_KEY = "fb4a_bottom_tabs_override_enabled"
private const val TRI_STATE_CLASS = "com.facebook.common.util.TriState"
private const val FB_SHARED_PREFERENCES = "com.facebook.prefs.shared.FbSharedPreferences"

/**
 * Resolves the static FbSharedPreferences PrefKey field initialized from OVERRIDE_KEY.
 */
val bottomTabOverrideKeyField = findFieldDirect {
    val clinits = findMethod {
        matcher {
            name = "<clinit>"
            strings(OVERRIDE_KEY)
        }
    }
    clinits.firstNotNullOf { clinit ->
        val code = clinit.instructions.toList()
        val strIdx = code.indexOfFirst { it.string == OVERRIDE_KEY }
        if (strIdx < 0) return@firstNotNullOf null
        code.drop(strIdx + 1).firstNotNullOfOrNull { inst ->
            if (inst.opcodeEnum != Opcode.SPUT_OBJECT) return@firstNotNullOfOrNull null
            inst.fieldRef?.takeIf {
                it.declaredClassName == clinit.declaredClassName && Modifier.isStatic(it.modifiers)
            }
        }
    }
}

/**
 * Concrete FbSharedPreferences implementation methods returning TriState.
 */
val fbSharedPrefsTriStateGetters = findMethodListDirect {
    findClass {
        matcher {
            addInterface(FB_SHARED_PREFERENCES)
        }
    }.findMethod {
        matcher {
            returnType = TRI_STATE_CLASS
        }
    }.filter { m ->
        !Modifier.isAbstract(m.modifiers) && !Modifier.isStatic(m.modifiers)
    }
}

/**
 * Tab bar at the bottom:
 * Moves Facebook's tab bar to the bottom of the screen on accounts that have it at the top.
 */
val TabBarAtTheBottom = patch(
    name = "Tab bar at the bottom",
    description = "Moves Facebook's tab bar to the bottom of the screen on accounts that have it at the top.",
) {
    val triStateClass = runCatching { classLoader.loadClass(TRI_STATE_CLASS) }.getOrNull()
    val yesConstant = runCatching { triStateClass?.getField("YES")?.get(null) }.getOrNull() ?: return@patch

    val overrideKeyObj = runCatching {
        ::bottomTabOverrideKeyField.field.apply { isAccessible = true }.get(null)
    }.getOrNull()

    runCatching {
        val hook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (!FacebookSettings.isEnabled(FacebookSettings.KEY_BOTTOM_TAB_BAR, false)) return

                val matchesKey = param.args?.any { arg ->
                    arg != null && ((overrideKeyObj != null && arg === overrideKeyObj) || arg.toString().contains(OVERRIDE_KEY))
                } ?: false

                if (matchesKey) {
                    Logger.printDebug { "TabBarAtTheBottom: Override tab bar position to YES" }
                    param.result = yesConstant
                }
            }
        }

        ::fbSharedPrefsTriStateGetters.dexMethodList.forEach { method ->
            runCatching { method.hookMethod(hook) }
        }
    }
}
