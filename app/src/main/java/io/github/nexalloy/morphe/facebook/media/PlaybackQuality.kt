package io.github.nexalloy.morphe.facebook.media

import java.util.Locale

enum class PlaybackQuality(val ceiling: Int, val fileValue: String, val displayName: String) {
    AUTO(-1, "auto", "Auto"),
    DATA_SAVER(0, "data_saver", "Data saver"),
    P480(480, "480p", "Up to 480p"),
    P720(720, "720p", "Up to 720p"),
    HIGHEST(Int.MAX_VALUE, "highest", "Highest");

    fun pick(labels: Iterable<String>?): String? {
        if (this == AUTO || labels == null) return null
        var picked: String? = null
        var pickedQuality = 0
        for (label in labels) {
            val quality = qualityOf(label)
            if (quality <= 0) continue
            if (picked == null || better(quality, pickedQuality)) {
                picked = label
                pickedQuality = quality
            }
        }
        return picked
    }

    private fun better(a: Int, b: Int): Boolean {
        val fitsA = a <= ceiling
        val fitsB = b <= ceiling
        if (fitsA != fitsB) return fitsA
        return if (fitsA) a > b else a < b
    }

    companion object {
        fun fromFile(value: String?): PlaybackQuality {
            if (value == null) return HIGHEST
            return entries.firstOrNull { it.fileValue == value } ?: HIGHEST
        }

        fun qualityOf(label: String?): Int {
            if (label == null) return 0
            val text = label.trim().lowercase(Locale.US)
            val digits = text.length - 1
            if (digits < 3 || digits > 4 || text[digits] != 'p') return 0
            var quality = 0
            for (i in 0 until digits) {
                val c = text[i]
                if (c !in '0'..'9') return 0
                quality = quality * 10 + (c - '0')
            }
            return quality
        }
    }
}
