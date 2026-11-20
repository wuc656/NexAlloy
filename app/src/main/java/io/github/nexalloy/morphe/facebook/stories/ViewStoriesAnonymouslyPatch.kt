package io.github.nexalloy.morphe.facebook.stories

import de.robv.android.xposed.XC_MethodHook
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.facebook.settings.FacebookSettings
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.opcodeEnum
import io.github.nexalloy.morphe.strings
import io.github.nexalloy.patch
import java.lang.reflect.Modifier

/**
 * Matches the sender method dispatching DirectSeenMutation (supports both <=585 query class and 586+ inline query building).
 */
val storySeenSenderMethod = findMethodDirect {
    val mutationClassName = findMethod {
        matcher {
            name = "<init>"
            strings("DirectSeenMutation", "direct_message_thread_update_seen_state")
        }
    }.firstOrNull()?.declaredClassName

    val builder = findMethod {
        matcher {
            strings("story_ids_list")
        }
    }.first { m ->
        !Modifier.isStatic(m.modifiers) &&
            m.returnTypeName != "void" &&
            if (mutationClassName != null) {
                m.instructions.any { it.opcodeEnum == Opcode.NEW_INSTANCE && it.classRef?.name == mutationClassName }
            } else {
                m.instructions.any { it.string == "DirectSeenMutation" }
            }
    }

    val owner = builder.declaredClass!!
    owner.methods.first { m ->
        !Modifier.isStatic(m.modifiers) &&
            m.returnTypeName == "void" &&
            m.descriptor != builder.descriptor &&
            m.paramTypeNames.any { it == "java.util.Set" } &&
            m.invokes.any { it.descriptor == builder.descriptor }
    }
}

val ViewStoriesAnonymously = patch(
    name = "View stories anonymously",
    description = "Keeps you off the viewer list of stories you watch.",
) {
    runCatching {
        ::storySeenSenderMethod.hookMethod(object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (FacebookSettings.isEnabled(FacebookSettings.KEY_VIEW_STORIES_ANONYMOUSLY, false)) {
                    param.result = null
                }
            }
        })
    }
}
