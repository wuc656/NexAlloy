package io.github.nexalloy.morphe.messenger.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.patch

val HideInboxAds = patch(
    name = "Hide inbox ads",
    description = "Removes inbox ad cards (InboxAdsItem) from Messenger's inbox.",
    use = true
) {
    val adItemClass = runCatching {
        classLoader.loadClass(INBOX_ADS_ITEM_CLASS)
    }.getOrNull()

    val copyOfMethod = runCatching {
        val immutableListClass = classLoader.loadClass(IMMUTABLE_LIST_CLASS)
        immutableListClass.declaredMethods.firstOrNull { m ->
            m.name == "copyOf" && m.parameterTypes.size == 1 && Collection::class.java.isAssignableFrom(m.parameterTypes[0])
        }
    }.getOrNull()

    InboxAdsProcessorFingerprint.hookMethod(object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val list = param.result as? List<*> ?: return
            if (list.isEmpty()) return

            var containsAd = false
            for (item in list) {
                if (item != null && (adItemClass?.isInstance(item) == true ||
                            item.javaClass.name == INBOX_ADS_ITEM_CLASS ||
                            item.javaClass.superclass?.name == INBOX_ADS_ITEM_CLASS)) {
                    containsAd = true
                    break
                }
            }

            if (!containsAd) return

            Logger.printDebug { "Messenger: Filtering inbox ads" }

            val filteredList = list.filter { item ->
                !(item != null && (adItemClass?.isInstance(item) == true ||
                        item.javaClass.name == INBOX_ADS_ITEM_CLASS ||
                        item.javaClass.superclass?.name == INBOX_ADS_ITEM_CLASS))
            }

            if (copyOfMethod != null) {
                param.result = copyOfMethod.invoke(null, filteredList)
            } else {
                param.result = filteredList
            }
        }
    })
}
