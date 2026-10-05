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
    const val KEY_HIDE_SPONSORED_PROFILE_POSTS = "hushfacebook_hide_sponsored_profile_posts"
    const val KEY_HIDE_POST_PROMPTS = "hushfacebook_hide_post_prompts"
    const val KEY_HIDE_META_AI_QUESTIONS = "hushfacebook_hide_meta_ai_questions"
    const val KEY_HIDE_FEEDS_HEADER = "hushfacebook_hide_feeds_header"
    const val KEY_KEEP_POST_DATES = "hushfacebook_keep_post_dates"
    const val KEY_HIDE_SPONSORED_SEARCH_RESULTS = "hushfacebook_hide_sponsored_search_results"
    const val KEY_BOTTOM_TAB_BAR = "hushfacebook_bottom_tab_bar"
    const val KEY_HIDE_SPONSORED_MARKETPLACE = "hushfacebook_hide_sponsored_marketplace"
    const val KEY_SANITIZE_SHARING_LINKS = "hushfacebook_sanitize_sharing_links"
    const val KEY_HIDE_SUGGESTED_STORIES = "hushfacebook_hide_suggested_stories"
    const val KEY_HIDE_REEL_PROMPTS = "hushfacebook_hide_reel_prompts"
    const val KEY_DONT_SEND_REEL_WATCH_HISTORY = "hushfacebook_dont_send_reel_watch_history"
    const val KEY_VIEW_STORIES_ANONYMOUSLY = "hushfacebook_view_stories_anonymously"
    const val KEY_STOP_STORY_AUTO_ADVANCE = "hushfacebook_stop_story_auto_advance"

    private var prefs: SharedPreferences? = null
    private var remotePrefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    fun initRemote(remote: SharedPreferences?) {
        remotePrefs = remote
    }

    fun isEnabled(key: String, default: Boolean = true): Boolean {
        // Check remote prefs (NexAlloy UI) first, then local prefs (Facebook Dialog), fallback to default
        if (remotePrefs != null && remotePrefs!!.contains(key)) {
            return remotePrefs!!.getBoolean(key, default)
        }
        return prefs?.getBoolean(key, default) ?: default
    }

    fun setEnabled(key: String, value: Boolean) {
        prefs?.edit()?.putBoolean(key, value)?.apply()
        remotePrefs?.edit()?.putBoolean(key, value)?.apply()
    }
}
