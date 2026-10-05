package io.github.nexalloy.morphe.facebook.stories

import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.patch

internal object StorySeenMutationFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("DirectSeenMutation", "direct_message_thread_update_seen_state")
)

val ViewStoriesAnonymously = patch(
    name = "View stories anonymously",
    description = "Keeps you off the viewer list of stories you watch.",
) {
    runCatching {
        StorySeenMutationFingerprint.hookMethod(object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (FacebookSettings.isEnabled(FacebookSettings.KEY_VIEW_STORIES_ANONYMOUSLY, false)) {
                    param.result = null
                }
            }
        })
    }
}
