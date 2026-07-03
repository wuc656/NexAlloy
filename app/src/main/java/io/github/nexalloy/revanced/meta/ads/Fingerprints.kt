package io.github.nexalloy.revanced.meta.ads

import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.strings

// Instagram — "Is ad pod" method returns boolean
val igAdInjectorFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "boolean"
            strings("Is ad pod")
        }
    }.single()
}

// Threads — "Is ad pod" method returns void
val threadsAdInjectorFingerprint = findMethodDirect {
    findMethod {
        matcher {
            returnType = "void"
            strings("Is ad pod")
        }
    }.single()
}