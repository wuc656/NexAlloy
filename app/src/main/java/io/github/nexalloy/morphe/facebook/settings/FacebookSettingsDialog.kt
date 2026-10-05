package io.github.nexalloy.morphe.facebook.settings

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

object FacebookSettingsDialog {

    private data class SettingItem(
        val key: String,
        val title: String,
        val summary: String,
        val defaultValue: Boolean = true
    )

    private val SETTING_ITEMS = listOf(
        SettingItem(
            FacebookSettings.KEY_HIDE_SPONSORED_POSTS,
            "Hide sponsored posts",
            "Removes sponsored and promoted posts from the news feed."
        ),
        SettingItem(
            FacebookSettings.KEY_HIDE_SUGGESTED_POSTS,
            "Hide suggested and promoted posts",
            "Removes suggested Pages, groups, and people you may know from the feed."
        ),
        SettingItem(
            FacebookSettings.KEY_HIDE_SPONSORED_REELS,
            "Hide sponsored reels",
            "Removes ads and sponsored items from Reels and Watch feeds."
        ),
        SettingItem(
            FacebookSettings.KEY_HIDE_REELS_IN_FEED,
            "Hide Reels in the feed",
            "Removes Reels trays and short-form video cards from the main feed."
        ),
        SettingItem(
            FacebookSettings.KEY_HIDE_SPONSORED_STORIES,
            "Hide sponsored stories",
            "Removes sponsored ad cards from the top Stories tray."
        ),
        SettingItem(
            FacebookSettings.KEY_HIDE_AFFILIATE_LINKS,
            "Hide affiliate product links",
            "Removes floating shopping cards and affiliate product overlays."
        ),
        SettingItem(
            FacebookSettings.KEY_BLOCK_PROMO_NOTIFS,
            "Block promotional notifications",
            "Stops notifications for birthdays, trending videos, memories, and page digests."
        ),
        SettingItem(
            FacebookSettings.KEY_BLOCK_AD_TELEMETRY,
            "Block ad telemetry",
            "Stops Facebook taking screenshots and reporting app installation attribution."
        ),
        SettingItem(
            FacebookSettings.KEY_BLOCK_AD_PREFETCH,
            "Block background ad prefetch",
            "Stops downloading ad creative and ad machine learning models in background."
        ),
        SettingItem(
            FacebookSettings.KEY_DISABLE_AUDIENCE_NETWORK,
            "Disable Audience Network",
            "Stops Facebook serving off-site ads and external tracking."
        ),
        SettingItem(
            FacebookSettings.KEY_HIDE_SPONSORED_PROFILE_POSTS,
            "Hide sponsored profile posts",
            "Removes ads between posts on user profiles and Pages."
        ),
        SettingItem(
            FacebookSettings.KEY_HIDE_POST_PROMPTS,
            "Hide post prompts",
            "Removes suggestions, who commented, and 'Are you interested' banners."
        ),
        SettingItem(
            FacebookSettings.KEY_HIDE_META_AI_QUESTIONS,
            "Hide Meta AI questions under posts",
            "Removes the row of Meta AI questions Facebook puts under posts."
        ),
        SettingItem(
            FacebookSettings.KEY_HIDE_FEEDS_HEADER,
            "Hide the Feeds header",
            "Removes the title row and filter pills (All, Favorites, Friends) from Feeds tab.",
            defaultValue = false
        ),
        SettingItem(
            FacebookSettings.KEY_KEEP_POST_DATES,
            "Keep post dates",
            "Keeps the post timestamp under the poster's name instead of rotating details."
        ),
        SettingItem(
            FacebookSettings.KEY_HIDE_SPONSORED_SEARCH_RESULTS,
            "Hide sponsored search results",
            "Removes ads and sponsored items from Facebook search results."
        ),
        SettingItem(
            FacebookSettings.KEY_BOTTOM_TAB_BAR,
            "Tab bar at the bottom",
            "Moves navigation tab bar to the bottom of the screen.",
            defaultValue = false
        ),
        SettingItem(
            FacebookSettings.KEY_HIDE_SPONSORED_MARKETPLACE,
            "Hide sponsored Marketplace listings",
            "Removes ads and boosted listings from Marketplace feed and search results."
        ),
        SettingItem(
            FacebookSettings.KEY_SANITIZE_SHARING_LINKS,
            "Sanitize sharing links",
            "Takes tracking tags (mibextid, fbclid, sfnsn) off links you share or copy."
        ),
        SettingItem(
            FacebookSettings.KEY_HIDE_SUGGESTED_STORIES,
            "Hide suggested stories",
            "Removes suggested stories and friend suggestions from Stories tray."
        ),
        SettingItem(
            FacebookSettings.KEY_HIDE_REEL_PROMPTS,
            "Hide reel interest prompts",
            "Removes 'Are you interested in this reel?' banner from Reels."
        ),
        SettingItem(
            FacebookSettings.KEY_DONT_SEND_REEL_WATCH_HISTORY,
            "Don't send reel watch history",
            "Stops sending Facebook the list of reels you have watched.",
            defaultValue = false
        ),
        SettingItem(
            FacebookSettings.KEY_VIEW_STORIES_ANONYMOUSLY,
            "View stories anonymously",
            "Keeps you off the viewer list of stories you watch.",
            defaultValue = false
        ),
        SettingItem(
            FacebookSettings.KEY_STOP_STORY_AUTO_ADVANCE,
            "Stop Story auto-advance",
            "Keeps each Story on screen until you tap or swipe.",
            defaultValue = false
        )
    )

    fun show(activity: Activity) {
        FacebookSettings.init(activity)

        val context = activity
        val dp = context.resources.displayMetrics.density

        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding((16 * dp).toInt(), (20 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt())
        }

        val titleView = TextView(context).apply {
            text = "Hushfacebook Settings"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, (8 * dp).toInt())
        }
        rootLayout.addView(titleView)

        val descView = TextView(context).apply {
            text = "NexAlloy Morphe Facebook integration. Toggle any feature and restart Facebook to apply."
            textSize = 12f
            setTextColor(Color.parseColor("#AAAAAA"))
            setPadding(0, 0, 0, (16 * dp).toInt())
        }
        rootLayout.addView(descView)

        val scrollView = ScrollView(context).apply {
            isFillViewport = true
        }

        val itemsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        for (item in SETTING_ITEMS) {
            val itemContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, (12 * dp).toInt(), 0, (12 * dp).toInt())
            }

            val textContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(0, 0, (8 * dp).toInt(), 0)
            }

            val itemTitle = TextView(context).apply {
                text = item.title
                textSize = 15f
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
            }
            val itemSummary = TextView(context).apply {
                text = item.summary
                textSize = 12f
                setTextColor(Color.parseColor("#888888"))
            }

            textContainer.addView(itemTitle)
            textContainer.addView(itemSummary)

            val toggle = Switch(context).apply {
                isChecked = FacebookSettings.isEnabled(item.key, item.defaultValue)
                setOnCheckedChangeListener { _, isChecked ->
                    FacebookSettings.setEnabled(item.key, isChecked)
                }
            }

            itemContainer.addView(textContainer)
            itemContainer.addView(toggle)
            itemsLayout.addView(itemContainer)

            val divider = View(context).apply {
                setBackgroundColor(Color.parseColor("#222222"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt().coerceAtLeast(1))
            }
            itemsLayout.addView(divider)
        }

        scrollView.addView(itemsLayout)
        rootLayout.addView(scrollView)

        AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setView(rootLayout)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
