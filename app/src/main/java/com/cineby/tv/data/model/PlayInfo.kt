package com.cineby.tv.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SubtitleTrack(
    val lang: String = "en",
    val label: String = "English",
    val path: String = ""
)

@Serializable
data class CinebySource(
    val url: String = "",
    val quality: String = "auto",
    val type: String = "hls", // 'hls' | 'mp4'
    val headers: Map<String, String> = emptyMap(),
    val server: String = ""
)

/**
 * Resolved stream ready for ExoPlayer consumption.
 * hlsMasterUrl is preferred; mp4Url is a direct-file fallback.
 */
@Serializable
data class ResolvedPlayback(
    val title: String = "",
    val hlsMasterUrl: String = "",
    val mp4Url: String? = null,
    val subtitles: List<SubtitleTrack> = emptyList(),
    val audioLanguage: String? = null,
    val durationSec: Long = 0L,
    val videoId: String = "",
    val server: String = "",
    val sources: List<CinebySource> = emptyList()
)
