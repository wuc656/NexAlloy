package io.github.nexalloy.morphe.twitch

import io.github.nexalloy.morphe.twitch.ads.BlockAudioAds
import io.github.nexalloy.morphe.twitch.ads.BlockLiveAds
import io.github.nexalloy.morphe.twitch.ads.BlockVideoAds
import io.github.nexalloy.morphe.twitch.ads.HideDisplayAds

val TwitchPatches = arrayOf(
    BlockLiveAds,
    HideDisplayAds,
    BlockVideoAds,
    BlockAudioAds,
)
