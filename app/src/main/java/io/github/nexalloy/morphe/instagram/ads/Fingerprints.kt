package io.github.nexalloy.morphe.instagram.ads

import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.strings
import java.lang.reflect.Modifier

internal val INJECTED_HASH = "injected".hashCode().toLong()

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
 * Media's getter for its "injected" ad details object.
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
            m.returnTypeName != "void" &&
            m.returnTypeName != "boolean" &&
            !m.returnTypeName.let { it == "int" || it == "long" }
    }
}
