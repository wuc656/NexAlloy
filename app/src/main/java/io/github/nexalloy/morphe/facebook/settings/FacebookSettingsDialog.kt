package io.github.nexalloy.morphe.facebook.settings

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

object FacebookSettingsDialog {

    data class SettingItem(
        val key: String,
        val title: String,
        val summary: String,
        val defaultValue: Boolean = true,
        val restartRequired: Boolean = false
    )

    data class Category(
        val title: String,
        val items: List<SettingItem>
    )

    private val CATEGORIES = listOf(
        Category(
            title = "News feed",
            items = listOf(
                SettingItem(
                    FacebookSettings.KEY_HIDE_SPONSORED_POSTS,
                    "Hide sponsored posts",
                    "Paid ads in the feed. They're dropped before Facebook adds them, so no gap is left."
                ),
                SettingItem(
                    FacebookSettings.KEY_HIDE_PROMOTED_POSTS,
                    "Hide promoted posts",
                    "Posts Facebook files as promotions rather than as ads."
                ),
                SettingItem(
                    FacebookSettings.KEY_HIDE_SPONSORED_PROFILE_POSTS,
                    "Hide sponsored profile posts",
                    "Ads between the posts on someone's profile or a Page. Their own posts stay."
                ),
                SettingItem(
                    FacebookSettings.KEY_HIDE_AFFILIATE_LINKS,
                    "Hide affiliate product links",
                    "The product cards of shop links creators add to posts, on reels, under feed posts and in the comments."
                ),
                SettingItem(
                    FacebookSettings.KEY_HIDE_SUGGESTED_POSTS,
                    "Hide suggested and promoted posts",
                    "Removes Pages you may like, groups, and people you may know from the feed."
                ),
                SettingItem(
                    FacebookSettings.KEY_HIDE_REELS_IN_FEED,
                    "Hide Reels in the feed",
                    "The rows of reels between posts, and the reels Facebook adds where your feed ends."
                ),
                SettingItem(
                    FacebookSettings.KEY_HIDE_POST_PROMPTS,
                    "Hide post prompts",
                    "The strip Facebook adds to some posts: suggestions, 'Are you interested in this post?', and recently commented."
                ),
                SettingItem(
                    FacebookSettings.KEY_HIDE_META_AI_QUESTIONS,
                    "Hide Meta AI questions under posts",
                    "Removes the row of Meta AI questions Facebook puts under posts."
                ),
                SettingItem(
                    FacebookSettings.KEY_KEEP_POST_DATES,
                    "Keep post dates",
                    "Keeps the one line with the date instead of rotating details."
                ),
                SettingItem(
                    FacebookSettings.KEY_HIDE_FEEDS_HEADER,
                    "Hide the Feeds header",
                    "Removes the title row and filter pills (All, Favorites, Friends) from Feeds tab.",
                    defaultValue = false,
                    restartRequired = true
                )
            )
        ),
        Category(
            title = "Stories",
            items = listOf(
                SettingItem(
                    FacebookSettings.KEY_HIDE_SPONSORED_STORIES,
                    "Hide sponsored stories",
                    "Removes ad cards from the story viewer, so swiping only shows stories people posted."
                ),
                SettingItem(
                    FacebookSettings.KEY_HIDE_SUGGESTED_STORIES,
                    "Hide suggested stories",
                    "Removes stories Facebook suggests from people and Pages you don't follow, and friend suggestions from Stories tray."
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
        ),
        Category(
            title = "Reels and Watch",
            items = listOf(
                SettingItem(
                    FacebookSettings.KEY_HIDE_SPONSORED_REELS,
                    "Hide sponsored reels",
                    "Ads inside Reels, starting with the next batch Facebook loads."
                ),
                SettingItem(
                    FacebookSettings.KEY_HIDE_REEL_PROMPTS,
                    "Hide reel interest prompts",
                    "No 'Are you interested in this reel?' prompt on reels. The reel plays as usual."
                ),
                SettingItem(
                    FacebookSettings.KEY_DONT_SEND_REEL_WATCH_HISTORY,
                    "Don't send reel watch history",
                    "Stop sending watched-reel lists to Facebook. It uses them to rank your feed.",
                    defaultValue = false
                )
            )
        ),
        Category(
            title = "Marketplace",
            items = listOf(
                SettingItem(
                    FacebookSettings.KEY_HIDE_SPONSORED_MARKETPLACE,
                    "Hide sponsored Marketplace listings",
                    "Ads and boosted listings in Marketplace's feed and search results."
                )
            )
        ),
        Category(
            title = "Search",
            items = listOf(
                SettingItem(
                    FacebookSettings.KEY_HIDE_SPONSORED_SEARCH_RESULTS,
                    "Hide sponsored search results",
                    "Ads between the results when you search Facebook. What you searched for stays."
                )
            )
        ),
        Category(
            title = "Notifications",
            items = listOf(
                SettingItem(
                    FacebookSettings.KEY_BLOCK_PROMO_NOTIFS,
                    "Block promotional notifications",
                    "Stops notifications for birthdays, trending videos, memories, and page digests."
                )
            )
        ),
        Category(
            title = "Appearance",
            items = listOf(
                SettingItem(
                    FacebookSettings.KEY_BOTTOM_TAB_BAR,
                    "Tab bar at the bottom",
                    "Put Facebook's tab bar at the bottom of the screen on accounts that have it at the top.",
                    defaultValue = false,
                    restartRequired = true
                )
            )
        ),
        Category(
            title = "Privacy & Misc",
            items = listOf(
                SettingItem(
                    FacebookSettings.KEY_SANITIZE_SHARING_LINKS,
                    "Sanitize sharing links",
                    "Takes tracking tags such as mibextid, fbclid, and sfnsn off the links you share or copy."
                ),
                SettingItem(
                    FacebookSettings.KEY_BLOCK_AD_PREFETCH,
                    "Block background ad prefetch",
                    "Facebook doesn't download ads or its ad model in the background."
                ),
                SettingItem(
                    FacebookSettings.KEY_BLOCK_AD_TELEMETRY,
                    "Block ad telemetry",
                    "No screenshot watching for ads, and no reports of which apps you install."
                ),
                SettingItem(
                    FacebookSettings.KEY_DISABLE_AUDIENCE_NETWORK,
                    "Disable Audience Network",
                    "Facebook doesn't serve ads to other apps on this phone."
                )
            )
        )
    )

    fun show(activity: Activity) {
        FacebookSettings.init(activity)
        val context = activity
        val dp = context.resources.displayMetrics.density

        var activeCategory: Category? = null
        var searchQuery = ""

        var dialog: AlertDialog? = null

        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Header View
        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * dp).toInt(), (14 * dp).toInt(), (16 * dp).toInt(), (12 * dp).toInt())
            setBackgroundColor(Color.parseColor("#1C1C1E"))
        }

        val backButton = TextView(context).apply {
            text = "←"
            textSize = 22f
            setTextColor(Color.WHITE)
            setPadding(0, 0, (16 * dp).toInt(), 0)
            visibility = View.GONE
        }

        val titleView = TextView(context).apply {
            text = "Hushfacebook"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeButton = TextView(context).apply {
            text = "✕"
            textSize = 18f
            setTextColor(Color.parseColor("#AAAAAA"))
            setPadding((8 * dp).toInt(), 0, (4 * dp).toInt(), 0)
            setOnClickListener { dialog?.dismiss() }
        }

        headerLayout.addView(backButton)
        headerLayout.addView(titleView)
        headerLayout.addView(closeButton)
        rootLayout.addView(headerLayout)

        // Search Box
        val searchBox = EditText(context).apply {
            hint = "Search settings..."
            setHintTextColor(Color.parseColor("#777777"))
            setTextColor(Color.WHITE)
            textSize = 14f
            setBackgroundColor(Color.parseColor("#262628"))
            setPadding((12 * dp).toInt(), (10 * dp).toInt(), (12 * dp).toInt(), (10 * dp).toInt())
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (8 * dp).toInt())
            }
            layoutParams = lp
        }
        rootLayout.addView(searchBox)

        val scrollView = ScrollView(context).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        val contentContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * dp).toInt(), (8 * dp).toInt(), (16 * dp).toInt(), (24 * dp).toInt())
        }
        scrollView.addView(contentContainer)
        rootLayout.addView(scrollView)

        fun render() {
            contentContainer.removeAllViews()

            if (searchQuery.isNotEmpty()) {
                backButton.visibility = View.VISIBLE
                titleView.text = "Search: $searchQuery"

                val matchedItems = CATEGORIES.flatMap { it.items }.filter {
                    it.title.contains(searchQuery, ignoreCase = true) || it.summary.contains(searchQuery, ignoreCase = true)
                }

                if (matchedItems.isEmpty()) {
                    val emptyView = TextView(context).apply {
                        text = "No matching settings found."
                        textSize = 14f
                        setTextColor(Color.parseColor("#888888"))
                        setPadding(0, (24 * dp).toInt(), 0, 0)
                    }
                    contentContainer.addView(emptyView)
                } else {
                    renderItemsList(context, dp, contentContainer, matchedItems)
                }
                return
            }

            if (activeCategory == null) {
                // Category Overview
                backButton.visibility = View.GONE
                titleView.text = "Hushfacebook"

                // Status Card at top
                val statusCard = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Color.parseColor("#1F2421"))
                    setPadding((14 * dp).toInt(), (12 * dp).toInt(), (14 * dp).toInt(), (12 * dp).toInt())
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = (16 * dp).toInt()
                    }
                    layoutParams = lp
                }
                val statusTitle = TextView(context).apply {
                    text = "● Hushfacebook Active"
                    setTextColor(Color.parseColor("#4CAF50"))
                    textSize = 14f
                    setTypeface(null, Typeface.BOLD)
                }
                val statusDesc = TextView(context).apply {
                    text = "Morphe DexKit + Xposed Framework running on Facebook 581."
                    setTextColor(Color.parseColor("#A5D6A7"))
                    textSize = 12f
                    setPadding(0, (4 * dp).toInt(), 0, 0)
                }
                statusCard.addView(statusTitle)
                statusCard.addView(statusDesc)
                contentContainer.addView(statusCard)

                for (category in CATEGORIES) {
                    val card = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setBackgroundColor(Color.parseColor("#1C1C1E"))
                        setPadding((16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt())
                        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            bottomMargin = (8 * dp).toInt()
                        }
                        layoutParams = lp
                        isClickable = true
                        isFocusable = true
                    }

                    val catInfo = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    val catTitle = TextView(context).apply {
                        text = category.title
                        textSize = 16f
                        setTextColor(Color.WHITE)
                        setTypeface(null, Typeface.BOLD)
                    }
                    val catSub = TextView(context).apply {
                        val count = category.items.size
                        text = "$count controls"
                        textSize = 12f
                        setTextColor(Color.parseColor("#888888"))
                        setPadding(0, (2 * dp).toInt(), 0, 0)
                    }
                    catInfo.addView(catTitle)
                    catInfo.addView(catSub)

                    val arrow = TextView(context).apply {
                        text = "›"
                        textSize = 22f
                        setTextColor(Color.parseColor("#666666"))
                    }

                    card.addView(catInfo)
                    card.addView(arrow)
                    card.setOnClickListener {
                        activeCategory = category
                        render()
                    }

                    contentContainer.addView(card)
                }
            } else {
                // Category Items View
                backButton.visibility = View.VISIBLE
                titleView.text = activeCategory!!.title

                renderItemsList(context, dp, contentContainer, activeCategory!!.items)
            }
        }

        backButton.setOnClickListener {
            if (searchQuery.isNotEmpty()) {
                searchBox.setText("")
            } else {
                activeCategory = null
                render()
            }
        }

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                render()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        render()

        dialog = AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setView(rootLayout)
            .show()
    }

    private fun renderItemsList(
        context: Activity,
        dp: Float,
        container: LinearLayout,
        items: List<SettingItem>
    ) {
        for (item in items) {
            val isNexAlloyEnabled = FacebookSettings.isPatchEnabledInNexAlloy(item.key)

            val itemCard = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(Color.parseColor("#1C1C1E"))
                setPadding((16 * dp).toInt(), (14 * dp).toInt(), (16 * dp).toInt(), (14 * dp).toInt())
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = (8 * dp).toInt()
                }
                layoutParams = lp
            }

            val textLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(0, 0, (12 * dp).toInt(), 0)
            }

            val itemTitle = TextView(context).apply {
                text = item.title
                textSize = 15f
                setTextColor(if (isNexAlloyEnabled) Color.WHITE else Color.parseColor("#777777"))
                setTypeface(null, Typeface.BOLD)
            }
            textLayout.addView(itemTitle)

            val itemSummary = TextView(context).apply {
                text = item.summary
                textSize = 12f
                setTextColor(if (isNexAlloyEnabled) Color.parseColor("#999999") else Color.parseColor("#555555"))
                setPadding(0, (2 * dp).toInt(), 0, 0)
            }
            textLayout.addView(itemSummary)

            if (!isNexAlloyEnabled) {
                val disabledBanner = TextView(context).apply {
                    text = "⚠️ Disabled by NexAlloy Patch switch"
                    textSize = 11f
                    setTextColor(Color.parseColor("#FF9800"))
                    setTypeface(null, Typeface.BOLD)
                    setPadding(0, (4 * dp).toInt(), 0, 0)
                }
                textLayout.addView(disabledBanner)
            } else if (item.restartRequired) {
                val restartNote = TextView(context).apply {
                    text = "Restart Facebook after changing"
                    textSize = 11f
                    setTextColor(Color.parseColor("#81C784"))
                    setPadding(0, (2 * dp).toInt(), 0, 0)
                }
                textLayout.addView(restartNote)
            }

            val toggle = Switch(context).apply {
                isEnabled = isNexAlloyEnabled
                isChecked = if (isNexAlloyEnabled) {
                    FacebookSettings.isEnabled(item.key, item.defaultValue)
                } else {
                    false
                }
                setOnCheckedChangeListener { _, isChecked ->
                    if (isNexAlloyEnabled) {
                        FacebookSettings.setEnabled(item.key, isChecked)
                    }
                }
            }

            itemCard.addView(textLayout)
            itemCard.addView(toggle)
            container.addView(itemCard)
        }
    }
}
