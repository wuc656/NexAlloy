package io.github.nexalloy.morphe.instagram.ads

import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.opcodeEnum
import io.github.nexalloy.morphe.strings
import java.lang.reflect.Modifier

internal val INJECTED_HASH = "injected".hashCode().toLong()

private val PRIMITIVE_OR_STRING_TYPES = setOf(
    "void", "boolean", "byte", "short", "char", "int", "long", "float", "double",
    "java.lang.String"
)

private val BUNDLE_CLASSES = setOf("android.os.BaseBundle", "android.os.Bundle")

val adInjectorFingerprint = findMethodDirect {
    findMethod {
        matcher {
            strings("cross_surface_duplicate_ad", "Is ad pod")
            returnType = "boolean"
            paramCount = 3
            modifiers = Modifier.STATIC
        }
    }.single()
}

/**
 * The parser that reads one home feed item from Instagram's JSON ("media_or_ad", "clips_netego").
 */
val feedItemParserFingerprint = findMethodDirect {
    findMethod {
        matcher {
            strings("media_or_ad", "clips_netego")
            returnType = "java.lang.Object"
            paramCount = 1
        }
    }.single()
}

/**
 * Media's getter for its "injected" ad details object (excludes String tracking-token getter added in 454+).
 */
val mediaAdDetailsGetter = findMethodDirect {
    findMethod {
        matcher {
            declaredClass("com.instagram.feed.media.Media")
            paramCount = 0
            usingNumbers(INJECTED_HASH)
        }
    }.first { m ->
        !Modifier.isStatic(m.modifiers) &&
            m.returnTypeName !in PRIMITIVE_OR_STRING_TYPES &&
            !m.returnTypeName.startsWith("[")
    }
}

/**
 * Boolean field in Instagram's comments-sheet settings serialized into Bundle under "enable_comment_ads".
 */
val commentAdsFlagField = findFieldDirect {
    val methods = findMethod {
        matcher {
            strings("enable_comment_ads")
        }
    }
    methods.firstNotNullOf { method ->
        val code = method.instructions.toList()
        var found: org.luckypray.dexkit.result.FieldData? = null
        for (at in code.indices) {
            if (code[at].string != "enable_comment_ads") continue
            val callIdx = ((at + 1)..minOf(code.lastIndex, at + 12)).firstOrNull { i ->
                val ref = code[i].methodRef ?: return@firstOrNull false
                ref.name == "putBoolean" && ref.declaredClassName in BUNDLE_CLASSES
            } ?: continue
            val getIdx = (callIdx - 1 downTo maxOf(0, callIdx - 8)).firstOrNull { i ->
                code[i].opcodeEnum == Opcode.IGET_BOOLEAN
            } ?: continue
            found = code[getIdx].fieldRef
            if (found != null) break
        }
        found
    }
}

/**
 * Methods serializing "enable_comment_ads" into a Bundle via putBoolean.
 */
val commentAdsBundleWriters = findMethodListDirect {
    findMethod {
        matcher {
            strings("enable_comment_ads")
        }
    }.filter { m ->
        m.invokes.any { it.name == "putBoolean" && it.declaredClassName in BUNDLE_CLASSES }
    }
}
