package io.github.nexalloy.morphe.instagram.ads

import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.strings

val adInjectorFingerprint = findMethodDirect {
    findMethod {
        matcher {
            strings("cross_surface_duplicate_ad", "Is ad pod")
            returnType = "boolean"
            paramCount = 3
            modifiers = java.lang.reflect.Modifier.STATIC
        }
    }.single()
}
