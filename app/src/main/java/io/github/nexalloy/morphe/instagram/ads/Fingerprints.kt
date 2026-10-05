package io.github.nexalloy.morphe.instagram.ads

import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.strings

/**
 * HushGram: The method that puts a sponsored item into a feed and answers whether it went in.
 * It writes the "Is ad pod" key into its debug map and flags a "cross_surface_duplicate_ad".
 * Static method with 3 parameters returning boolean.
 */
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
