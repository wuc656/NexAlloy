package io.github.nexalloy.morphe.facebook.notifications

import app.morphe.extension.shared.Logger
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.patch

internal object TrayManagerPostFingerprint : Fingerprint(
    definingClass = "com.facebook.notifications.tray.SystemTrayNotificationManager",
    returnType = "V",
    strings = listOf("show_notif_start")
)

val BlockPromotionalNotifications = patch(
    name = "Block promotional notifications",
    description = "Keeps promotional and noise notifications off your phone.",
) {
    TrayManagerPostFingerprint.hookMethod {
        before { param ->
            if (param.args == null || param.args.size < 4) return@before
            val builder = param.args[3] ?: return@before
            
            // The builder has a field of type SystemTrayNotification. Let's find it.
            val builderClass = builder.javaClass
            val notificationField = builderClass.declaredFields.firstOrNull { 
                it.type.name == "com.facebook.notifications.push.model.SystemTrayNotification" 
            } ?: return@before
            
            notificationField.isAccessible = true
            val notification = notificationField.get(builder) ?: return@before
            
            val notificationClass = notification.javaClass
            val typeField = notificationClass.declaredFields.firstOrNull { it.name == "mType" && it.type == String::class.java }
                ?: return@before
                
            typeField.isAccessible = true
            val type = typeField.get(notification) as? String ?: return@before
            
            val colon = type.indexOf(':')
            val head = if (colon >= 0) type.substring(0, colon) else type
            val kind = head.uppercase()
            
            val blockedKinds = setOf(
                "TOP_TRENDING_VIDEO",
                "PERSONALIZED_REELS",
                "ONTHISDAY",
                "BIRTHDAY_REMINDER",
                "GROUP_HIGHLIGHTS",
                "GROUP_NF_HIGHLIGHTS",
                "PAGE_HIGHLIGHTS",
                "CREATOR_HIGHLIGHTS",
                "PYMK_EMAIL",
                "PLACE_FEED_NEARBY",
                "NEAR_SAVED_PLACE",
                "WEATHER_NOWCAST"
            )
            
            if (kind in blockedKinds) {
                Logger.printDebug { "Block promotional notifications: Blocked $kind" }
                param.result = null
            }
        }
    }
}
