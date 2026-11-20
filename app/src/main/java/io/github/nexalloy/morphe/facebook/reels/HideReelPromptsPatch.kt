package io.github.nexalloy.morphe.facebook.reels

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.opcodeEnum
import io.github.nexalloy.morphe.strings
import io.github.nexalloy.patch

private const val INTEREST_PROMPT = "INTERESTED_OR_NOT_INTERESTED_BUMPER"
private const val TUNE_YOUR_ALGORITHM = "TUNE_YOUR_ALGORITHM"

/**
 * Matches the reel overlay predicate that checks whether a reel gets the
 * INTERESTED_OR_NOT_INTERESTED_BUMPER ("Are you interested in this reel?") prompt.
 */
val reelPromptCheckMethod = findMethodDirect {
    val overlayEnum = findClass {
        matcher {
            superClass("java.lang.Enum")
            addMethod {
                name = "<clinit>"
                strings(INTEREST_PROMPT, TUNE_YOUR_ALGORITHM)
            }
        }
    }.single()

    val clinit = overlayEnum.methods.first { it.name == "<clinit>" }
    val clinitCode = clinit.instructions.toList()
    val strIndex = clinitCode.indexOfFirst { it.string == INTEREST_PROMPT }
    require(strIndex >= 0) { "$INTEREST_PROMPT not found in ${overlayEnum.name}.<clinit>" }

    val promptField = clinitCode.drop(strIndex + 1).firstNotNullOf { inst ->
        if (inst.opcodeEnum != Opcode.SPUT_OBJECT) return@firstNotNullOf null
        inst.fieldRef?.takeIf { it.declaredClassName == overlayEnum.name }
    }

    findMethod {
        matcher {
            returnType = "boolean"
            addUsingField {
                declaredClass(overlayEnum.name)
                name = promptField.name
            }
        }
    }.first { m ->
        if (m.paramTypeNames.isEmpty()) return@first false
        val firstParam = m.paramTypeNames[0]
        val code = m.instructions.toList()
        (0 until code.lastIndex).any { i ->
            val inst = code[i]
            val next = code[i + 1]
            inst.opcodeEnum == Opcode.SGET_OBJECT &&
                inst.fieldRef?.descriptor == promptField.descriptor &&
                next.methodRef?.let { ref ->
                    ref.returnTypeName == "boolean" &&
                        ref.paramTypeNames == listOf(overlayEnum.name, firstParam)
                } == true
        }
    }
}

val HideReelPrompts = patch(
    name = "Hide reel interest prompts",
    description = "Removes the 'Are you interested in this reel?' prompt from reels.",
) {
    runCatching {
        ::reelPromptCheckMethod.hookMethod(object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (FacebookSettings.isEnabled(FacebookSettings.KEY_HIDE_REEL_PROMPTS, true)) {
                    Logger.printDebug { "HideReelPrompts: Suppressed reel interest prompt" }
                    param.result = false
                }
            }
        })
    }
}
