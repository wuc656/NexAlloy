/*
 * Copyright 2026 HushTelegram contributors
 * https://github.com/SysAdminDoc/HushTelegram
 */
package io.github.nexalloy.morphe.telegram.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.callMethodOrNull
import io.github.nexalloy.patch

private const val STASHED_LEFT_EXTRA = "hush_stashed_left"

val HideTelegramSponsoredProxy = patch(
    name = "Hide sponsored proxy channel",
    description = "Hides the sponsored channel added by an MTProto proxy from your chat list and folders without changing proxy settings.",
    use = true
) {
    val controllerClass = runCatching { classLoader.loadClass(MESSAGES_CONTROLLER_CLASS) }.getOrNull() ?: return@patch
    val dialogClass = runCatching { classLoader.loadClass("org.telegram.tgnet.TLRPC\$Dialog") }.getOrNull() ?: return@patch
    val chatClass = runCatching { classLoader.loadClass("org.telegram.tgnet.TLRPC\$Chat") }.getOrNull() ?: return@patch

    val promoDialogField = runCatching { controllerClass.getDeclaredField("promoDialog").apply { isAccessible = true } }.getOrNull() ?: return@patch
    val isLeftPromoChannelField = runCatching { controllerClass.getDeclaredField("isLeftPromoChannel").apply { isAccessible = true } }.getOrNull() ?: return@patch
    val promoDialogTypeField = runCatching { controllerClass.getDeclaredField("promoDialogType").apply { isAccessible = true } }.getOrNull() ?: return@patch
    val isPromoDialogMethod = runCatching {
        controllerClass.getDeclaredMethod("isPromoDialog", Long::class.javaPrimitiveType, Boolean::class.javaPrimitiveType).apply { isAccessible = true }
    }.getOrNull() ?: return@patch
    val getChatMethod = runCatching {
        controllerClass.getDeclaredMethod("getChat", java.lang.Long::class.java).apply { isAccessible = true }
    }.getOrNull() ?: return@patch
    val dialogIdField = runCatching { dialogClass.getField("id").apply { isAccessible = true } }.getOrNull() ?: return@patch
    val chatLeftField = runCatching { chatClass.getField("left").apply { isAccessible = true } }.getOrNull() ?: return@patch

    // 13.1.1 kept PROMO_TYPE_PROXY static field; 13.2+ inlined its constant (0).
    val proxyTypeValue = runCatching {
        controllerClass.getDeclaredField("PROMO_TYPE_PROXY").apply { isAccessible = true }.getInt(null)
    }.getOrDefault(0)

    fun isSponsoredProxyDialog(controller: Any, dialog: Any): Boolean = runCatching {
        if (promoDialogTypeField.getInt(controller) != proxyTypeValue) return@runCatching false
        val dialogId = dialogIdField.getLong(dialog)
        if (dialogId >= 0L) return@runCatching false
        if (isPromoDialogMethod.invoke(controller, dialogId, false) != true) return@runCatching false
        val chat = getChatMethod.invoke(controller, -dialogId) ?: return@runCatching false
        chatLeftField.getBoolean(chat)
    }.getOrDefault(false)

    // 1. Prevent sortDialogs from reinserting cached sponsored proxy channel into allDialogs / folders
    runCatching {
        ::sortDialogsMethod.hookMethod(object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val controller = param.thisObject ?: return
                val promoDialog = promoDialogField.get(controller) ?: return
                if (!isLeftPromoChannelField.getBoolean(controller)) return
                if (isSponsoredProxyDialog(controller, promoDialog)) {
                    isLeftPromoChannelField.setBoolean(controller, false)
                    param.setObjectExtra(STASHED_LEFT_EXTRA, true)
                    Logger.printDebug { "Telegram: Suppressed cached sponsored proxy dialog in sortDialogs" }
                }
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                if (param.getObjectExtra(STASHED_LEFT_EXTRA) == true) {
                    val controller = param.thisObject ?: return
                    runCatching { isLeftPromoChannelField.setBoolean(controller, true) }
                }
            }
        })
    }

    // 2. Hide sponsored proxy dialog from custom DialogFilter folder rebuilds
    // Telegram 13.3.1 added a trailing boolean parameter to DialogFilter.includesDialog.
    runCatching {
        val dialogFilterClass = classLoader.loadClass("org.telegram.messenger.MessagesController\$DialogFilter")
        val accountInstanceClass = classLoader.loadClass("org.telegram.messenger.AccountInstance")
        val includesHook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (param.result != true) return
                val controller = param.args?.getOrNull(0)?.callMethodOrNull("getMessagesController") ?: return
                val dialog = param.args.getOrNull(2) ?: return
                if (isSponsoredProxyDialog(controller, dialog)) {
                    Logger.printDebug { "Telegram: Hid sponsored proxy channel from DialogFilter folder" }
                    param.result = false
                }
            }
        }
        dialogFilterClass.declaredMethods.filter { m ->
            m.name == "includesDialog" && m.returnType == Boolean::class.javaPrimitiveType &&
                m.parameterTypes.size in 3..4 &&
                m.parameterTypes[0] == accountInstanceClass &&
                m.parameterTypes[1] == Long::class.javaPrimitiveType &&
                m.parameterTypes[2] == dialogClass &&
                (m.parameterTypes.size == 3 || m.parameterTypes[3] == Boolean::class.javaPrimitiveType)
        }.forEach { XposedBridge.hookMethod(it, includesHook) }
    }
}
