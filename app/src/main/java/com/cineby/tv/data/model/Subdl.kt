package com.cineby.tv.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubdlResponse(
    val status: Boolean = false,
    val subtitles: List<SubdlSubtitle> = emptyList()
)


@Serializable
data class SubdlSubtitle(
    @SerialName("release_name") val releaseName: String? = null,
    val name: String? = null,
    val lang: String? = null,
    val author: String? = null,
    val url: String = "",
    val language: String? = null,
    val season: Int? = null,
    val episode: Int? = null
) {
    val fullDownloadUrl: String
        get() = if (url.startsWith("http")) url else "https://dl.subdl.com$url"
}
