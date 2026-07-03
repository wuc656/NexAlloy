package io.github.nexalloy.facebook.ads

import io.github.nexalloy.morphe.findMethodDirect

// Match methods in AdBucketDataSourceUtil-related classes
// These classes contain the ad insertion/fetch logic and have distinctive field names
val adsInsertionFingerprint = findMethodDirect {
    findClass {
        matcher {
            className("AdBucketDataSourceUtil")
        }
    }.findMethod {
        matcher {
            returnType = "void"
            name = "run"
        }
    }.first()
}

val fetchMoreAdsFingerprint = findMethodDirect {
    findClass {
        matcher {
            className("AdBucketDataSourceUtil")
        }
    }.findMethod {
        matcher {
            returnType = "void"
            name = "run"
        }
    }.last()
}