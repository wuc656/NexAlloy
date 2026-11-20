package io.github.nexalloy.morphe.messenger.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val HideInboxAds = patch(
    name = "Hide inbox ads",
    description = "Removes inbox ad cards (InboxAdsItem) and suppresses ad entry events, inbox ad impressions, and ad attribution uploads in Messenger.",
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

    fun isInboxAdItem(item: Any?): Boolean {
        if (item == null) return false
        return adItemClass?.isInstance(item) == true ||
            item.javaClass.name == INBOX_ADS_ITEM_CLASS ||
            item.javaClass.superclass?.name == INBOX_ADS_ITEM_CLASS
    }

    // 1. Filter InboxAdsItem entries from ItemListProcessorInterfaceSpec.processItems
    runCatching {
        InboxAdsProcessorFingerprint.hookMethod(object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val list = param.result as? List<*> ?: return
                if (list.isEmpty()) return
                if (list.none(::isInboxAdItem)) return

                Logger.printDebug { "Messenger: Filtering inbox ads" }
                val filteredList = list.filterNot(::isInboxAdItem)
                param.result = if (copyOfMethod != null) {
                    copyOfMethod.invoke(null, filteredList)
                } else {
                    filteredList
                }
            }
        })
    }

    // 1b. Legacy Messenger (<=585) inbox ads supplier
    runCatching {
        LoadInboxAdsFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
    }

    // 2. Block click-to-message ad entry event ("ad_id", "page_id")
    runCatching {
        AdEntryEventFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
    }

    // 3. Block InboxCustomizedImpressionTracker ("inbox2_vr") submission while keeping tracker state and ReqContext close
    runCatching {
        val loggerMember = ::inboxVisibilityLoggerMethod.member
        InboxVisibilityEventFingerprint.hookMethod(
            scopedHook(loggerMember) {
                before { param ->
                    Logger.printDebug { "Messenger: Blocked inbox2_vr ad impression event" }
                    param.result = null
                }
            }
        )
    }

    // 4. Block LatStatusJob ad attribution upload worker
    runCatching {
        ::attributionUploadMethod.hookMethod(XC_MethodReplacement.DO_NOTHING)
    }
}
