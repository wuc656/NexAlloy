package io.github.nexalloy.morphe.facebook.settings

import android.content.Context
import android.content.SharedPreferences

object FacebookSettings {
    private const val PREF_NAME = "hushfacebook_prefs"

    // Default values matched with Hushfacebook
    const val KEY_HIDE_SPONSORED_POSTS = "hushfacebook_hide_sponsored_posts"
    const val KEY_HIDE_SPONSORED_REELS = "hushfacebook_hide_sponsored_reels"
    const val KEY_HIDE_SPONSORED_STORIES = "hushfacebook_hide_sponsored_stories"
    const val KEY_HIDE_AFFILIATE_LINKS = "hushfacebook_hide_affiliate_links"
    const val KEY_BLOCK_PROMO_NOTIFS = "hushfacebook_block_promo_notifications"
    const val KEY_BLOCK_AD_TELEMETRY = "hushfacebook_block_ad_telemetry"
    const val KEY_BLOCK_AD_PREFETCH = "hushfacebook_block_ad_prefetch"
    const val KEY_DISABLE_AUDIENCE_NETWORK = "hushfacebook_disable_audience_network"
    const val KEY_HIDE_REELS_IN_FEED = "hushfacebook_hide_reels_in_feed"
    const val KEY_HIDE_SUGGESTED_POSTS = "hushfacebook_hide_suggested_posts"
    const val KEY_FORCE_DARK_MODE = "hushfacebook_force_dark_mode"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    fun isEnabled(key: String, default: Boolean = true): Boolean {
        return prefs?.getBoolean(key, default) ?: default
    }

    fun setEnabled(key: String, value: Boolean) {
        prefs?.edit()?.putBoolean(key, value)?.apply()
    }
}
