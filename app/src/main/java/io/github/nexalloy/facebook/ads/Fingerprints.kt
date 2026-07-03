package io.github.nexalloy.facebook.ads

import io.github.nexalloy.morphe.findMethodDirect

private const val REDEX_ORIGINAL_NAME_FIELD = "__redex_internal_original_name"

private fun runMethod(originalName: String) = findMethodDirect {
    findClass {
        matcher {
            addInterface("java.lang.Runnable")
            addField {
                name(REDEX_ORIGINAL_NAME_FIELD)
                type("java.lang.String")
            }
            addEqString(originalName)
        }
    }.findMethod {
        matcher {
            name("run")
            returnType("void")
            paramTypes()
        }
    }.single()
}

// Match Runnable.run() in AdBucketDataSourceUtil$attemptAdsInsertion$1.
val adsInsertionFingerprint = runMethod("AdBucketDataSourceUtil\$attemptAdsInsertion\$1")

// Match Runnable.run() in AdBucketDataSourceUtil$attemptFetchMoreAds$1.
val fetchMoreAdsFingerprint = runMethod("AdBucketDataSourceUtil\$attemptFetchMoreAds\$1")
