/*
 * Copyright 2026 HushThreads contributors
 * https://github.com/SysAdminDoc/HushThreads
 */
package io.github.nexalloy.morphe.threads.ads

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.opcodeEnum
import io.github.nexalloy.morphe.strings
import org.luckypray.dexkit.result.MethodData
import java.lang.reflect.Modifier

internal const val FEED_CACHE = "Lcom/instagram/barcelona/feed/data/cache/BarcelonaFeedCache;"
internal const val FEED_CACHE_CLASS = "com.instagram.barcelona.feed.data.cache.BarcelonaFeedCache"
internal const val MEDIA = "Lcom/instagram/feed/media/Media;"
internal const val MEDIA_CLASS = "com.instagram.feed.media.Media"
internal val INJECTED_FIELD = "injected".hashCode()
internal const val PERMALINK_AD_FETCHER = "Lcom/instagram/barcelona/sponsored/permalink/ThreadsPermalinkAdFetcher;"
internal const val PERMALINK_ADS_PATH = "text_feed/threads_permalink_ads/"
internal const val FOR_YOU_LOCAL_SOURCE_CLASS = "com.instagram.barcelona.feed.data.cache.ForYouFeedLocalDataSource"
internal const val FEED_CACHE_HANDLER_CLASS = "com.instagram.barcelona.feed.data.cache.BarcelonaFeedCacheHandler"

private fun MethodData.findItemClassName(): String? = instructions.firstNotNullOfOrNull { inst ->
    inst.methodRef?.takeIf { ref ->
        ref.returnTypeName == MEDIA_CLASS &&
            ref.paramTypeNames.isEmpty() &&
            ref.declaredClassName != MEDIA_CLASS
    }?.declaredClassName
}

/**
 * BarcelonaFeedCache.addAndSaveItemsFromFeedFetchSuccess:
 * Merges a fetched feed page into the feed cache.
 */
val feedPageMergeMethod = findMethodDirect {
    findMethod {
        matcher {
            declaredClass(FEED_CACHE_CLASS)
            name = "addAndSaveItemsFromFeedFetchSuccess"
        }
    }.firstOrNull() ?: findMethod {
        matcher {
            declaredClass(FEED_CACHE_CLASS)
        }
    }.firstOrNull { method ->
        method.paramTypeNames.any { it.contains("List") } &&
            method.instructions.any { it.methodRef?.declaredClassName?.contains("addAndSaveItemsFromFeedFetchSuccess") == true }
    } ?: findMethod {
        matcher {
            declaredClass(FEED_CACHE_CLASS)
        }
    }.first { method ->
        method.paramTypeNames.any { it.contains("List") }
    }
}

/**
 * Additional For You suspend list writes in BarcelonaFeedCache (refresh, paging, append, rebuild)
 * that bypass addAndSaveItemsFromFeedFetchSuccess.
 */
val forYouListWriteMethods = findMethodListDirect {
    val merge = feedPageMergeMethod()
    val continuationType = merge.paramTypeNames.firstOrNull { it.contains("Continuation") }
        ?: merge.paramTypeNames.getOrNull(6)
    val allowedBesides = buildSet {
        merge.paramTypeNames.firstOrNull()?.let(::add)
        add("java.lang.Integer")
        add("kotlin.jvm.functions.Function0")
        add("boolean")
        add("java.util.List")
        continuationType?.let(::add)
    }
    findMethod {
        matcher {
            declaredClass(FEED_CACHE_CLASS)
            returnType = "java.lang.Object"
        }
    }.filter { m ->
        !Modifier.isStatic(m.modifiers) &&
            !Modifier.isPrivate(m.modifiers) &&
            m.descriptor != merge.descriptor &&
            m.paramTypeNames.count { it == "java.util.List" } == 1 &&
            (continuationType == null || m.paramTypeNames.count { it == continuationType } == 1) &&
            m.paramTypeNames.all { it in allowedBesides }
    }
}

/**
 * Single-item suspend writes in BarcelonaFeedCache (add/insert after/swap single feed item).
 */
val forYouItemWriteMethods = findMethodListDirect {
    val itemClass = feedPageMergeMethod().findItemClassName() ?: return@findMethodListDirect emptyList()
    findMethod {
        matcher {
            declaredClass(FEED_CACHE_CLASS)
            returnType = "java.lang.Object"
        }
    }.filter { m ->
        !Modifier.isStatic(m.modifiers) &&
            !Modifier.isPrivate(m.modifiers) &&
            m.paramTypeNames.firstOrNull() == itemClass
    }
}

/**
 * Bare Unit-like singleton field returned by single-item cache writes on early exit.
 */
val forYouDoneUnitField = findFieldDirect {
    val writes = forYouItemWriteMethods()
    val perMethod = writes.map { write ->
        write.instructions.mapNotNull { inst ->
            if (inst.opcodeEnum != Opcode.SGET_OBJECT) return@mapNotNull null
            val field = inst.fieldRef ?: return@mapNotNull null
            if (field.typeName != field.declaredClassName) return@mapNotNull null
            val holder = field.declaredClass ?: return@mapNotNull null
            val isBareSingleton = holder.superClass?.name == "java.lang.Object" &&
                holder.interfaces.isEmpty() &&
                holder.fields.all { Modifier.isStatic(it.modifiers) } &&
                holder.fields.count { it.typeName == holder.name } == 1
            if (isBareSingleton) field else null
        }.distinctBy { it.descriptor }
    }
    val common = if (perMethod.isNotEmpty()) {
        val sharedDescriptors = perMethod.map { list -> list.map { it.descriptor }.toSet() }
            .reduce { acc, next -> acc intersect next }
        perMethod.flatten().firstOrNull { it.descriptor in sharedDescriptors }
    } else {
        null
    }
    common ?: perMethod.flatten().first()
}

/**
 * For You standalone single-ad insert method taking an object holding the item and an int position.
 */
val forYouAdInsertMethod = findMethodDirect {
    findClass {
        matcher {
            addField { type = FOR_YOU_LOCAL_SOURCE_CLASS }
        }
    }.findMethod {
        matcher {
            returnType = "java.lang.Integer"
        }
    }.first { m ->
        m.paramTypeNames.lastOrNull() == "int" &&
            m.instructions.any { inst ->
                inst.opcodeEnum == Opcode.CHECK_CAST && inst.classRef?.name == FEED_CACHE_HANDLER_CLASS
            }
    }
}

/**
 * Static boxed Integer result code returned by forYouAdInsertMethod when no page is present.
 */
val forYouAdInsertNoPageField = findFieldDirect {
    val insert = forYouAdInsertMethod()
    val code = insert.instructions.toList()
    val checkIdx = (0 until code.lastIndex).firstOrNull { i ->
        code[i].opcodeEnum == Opcode.CHECK_CAST && code[i + 1].opcodeEnum == Opcode.IF_EQZ
    }
    val afterCheck = if (checkIdx != null) code.drop(checkIdx + 2) else code
    afterCheck.firstNotNullOfOrNull { inst ->
        inst.takeIf { it.opcodeEnum == Opcode.SGET_OBJECT }?.fieldRef?.takeIf {
            it.typeName == "java.lang.Integer"
        }
    } ?: code.firstNotNullOf { inst ->
        inst.takeIf { it.opcodeEnum == Opcode.SGET_OBJECT }?.fieldRef?.takeIf {
            it.typeName == "java.lang.Integer"
        }
    }
}

/**
 * InjectedAdCheckFingerprint:
 * Threads' check of whether a post carries the "injected" block the server puts on an ad.
 * Static boolean method taking the post's fragment.
 * Uses 0x8669a9b0 and INJECTED_FIELD ("injected".hashCode()).
 */
internal object InjectedAdCheckFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("L"),
    filters = listOf(
        literal(0x8669a9b0.toInt()),
        literal(INJECTED_FIELD)
    )
)

/**
 * Media's own ad check method: the unique non-static boolean getter in Media
 * that invokes InjectedAdCheckFingerprint.
 */
val mediaIsAdMethod = findMethodDirect {
    val injected = InjectedAdCheckFingerprint.run()
    findMethod {
        matcher {
            declaredClass(MEDIA_CLASS)
            returnType = "boolean"
            paramCount = 0
        }
    }.first { m ->
        !Modifier.isStatic(m.modifiers) &&
            m.invokes.any { it.declaredClassName == injected.declaredClassName && it.name == injected.name }
    }
}

/**
 * PermalinkAdFetchFingerprint:
 * Fetches ads for an opened post's replies.
 */
internal object PermalinkAdFetchFingerprint : Fingerprint(
    definingClass = PERMALINK_AD_FETCHER,
    returnType = "Ljava/lang/Object;",
    strings = listOf(PERMALINK_ADS_PATH)
)

val permalinkAdResultConstructor = findMethodDirect {
    val fetchMethod = PermalinkAdFetchFingerprint.run()
    val initInstruction = fetchMethod.instructions.firstOrNull { inst ->
        val ref = inst.methodRef ?: return@firstOrNull false
        ref.isConstructor && ref.paramTypeNames.size == 2 &&
            ref.paramTypeNames[0].contains("List") &&
            ref.paramTypeNames[1] == "int"
    } ?: error("Constructor not found in PermalinkAdFetchMethod")
    initInstruction.methodRef!!
}

val adInjectorFingerprint = findMethodDirect {
    findMethod {
        matcher {
            strings("cross_surface_duplicate_ad", "Is ad pod")
            returnType = "boolean"
            paramCount = 3
            modifiers = Modifier.STATIC
        }
    }.firstOrNull() ?: findMethod {
        matcher {
            strings("Is ad pod")
            returnType = "boolean"
        }
    }.first()
}
