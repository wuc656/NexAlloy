package io.github.nexalloy.facebook.ads

import io.github.nexalloy.morphe.findMethodDirect

// Match Runnable.run() in AdBucketDataSourceUtil$attemptAdsInsertion$1
val adsInsertionFingerprint = findMethodDirect {
    findClass {
        matcher {
            className("AdBucketDataSourceUtil\$attemptAdsInsertion")
        }
    }.findMethod {
        matcher {
            returnType = "void"
            name = "run"
        }
    }.single()
}

// Match Runnable.run() in AdBucketDataSourceUtil$attemptFetchMoreAds$1
val fetchMoreAdsFingerprint = findMethodDirect {
    findClass {
        matcher {
            className("AdBucketDataSourceUtil\$attemptFetchMoreAds")
        }
    }.findMethod {
        matcher {
            returnType = "void"
            name = "run"
        }
    }.single()
}