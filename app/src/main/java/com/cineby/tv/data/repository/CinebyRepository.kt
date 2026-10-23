package com.cineby.tv.data.repository

import com.cineby.tv.data.api.ApiClient
import com.cineby.tv.data.api.CinebyApiService
import com.cineby.tv.data.model.EpisodeResponse
import com.cineby.tv.data.model.MovieDetail
import com.cineby.tv.data.model.MovieItem
import com.cineby.tv.data.model.ResolvedPlayback
import com.cineby.tv.data.model.SearchResultItem
import com.cineby.tv.data.model.SeriesDetail
import com.cineby.tv.data.model.SeriesItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class CinebyRepository(
    private val cinebyApi: CinebyApiService = ApiClient.cinebyApi
) {

    suspend fun getTrendingMovies(): List<MovieItem> = withContext(Dispatchers.IO) {
        try {
            cinebyApi.getMovies(page = 1, limit = 20, sort = "popularity").data
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPopularMovies(): List<MovieItem> = withContext(Dispatchers.IO) {
        try {
            cinebyApi.getMovies(page = 1, limit = 20).data
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMoviesBySort(sort: String): List<MovieItem> = withContext(Dispatchers.IO) {
        try {
            cinebyApi.getMovies(page = 1, limit = 20, sort = sort).data
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMoviesByGenre(genreSlug: String): List<MovieItem> = withContext(Dispatchers.IO) {
        try {
            cinebyApi.getMovies(page = 1, limit = 20, genre = genreSlug).data
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPopularSeries(): List<SeriesItem> = withContext(Dispatchers.IO) {
        try {
            cinebyApi.getSeries(page = 1, limit = 20, sort = "popularity").data
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSeriesBySort(sort: String): List<SeriesItem> = withContext(Dispatchers.IO) {
        try {
            cinebyApi.getSeries(page = 1, limit = 20, sort = sort).data
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSeriesByGenre(genreSlug: String): List<SeriesItem> = withContext(Dispatchers.IO) {
        try {
            cinebyApi.getSeries(page = 1, limit = 20, genre = genreSlug).data
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMovieDetail(slug: String): MovieDetail? = withContext(Dispatchers.IO) {
        try {
            cinebyApi.getMovieDetail(slug)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSeriesDetail(slug: String): SeriesDetail? = withContext(Dispatchers.IO) {
        try {
            cinebyApi.getSeriesDetail(slug)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getEpisodeDetail(slug: String, season: Int, episode: Int): EpisodeResponse? = withContext(Dispatchers.IO) {
        try {
            cinebyApi.getEpisodeDetail(slug, season, episode)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun search(query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        try {
            cinebyApi.search(trimmed).results
        } catch (e: Exception) {
            android.util.Log.w("CinebyRepository", "Backend search error: ${e.message}")
            emptyList()
        }
    }

    private data class CachedPlayback(
        val playback: ResolvedPlayback,
        val timestamp: Long = System.currentTimeMillis()
    )

    companion object {
        private val playbackCache = ConcurrentHashMap<String, CachedPlayback>()
        private val activePrewarms = ConcurrentHashMap<String, Deferred<Result<ResolvedPlayback>>>()
        private const val CACHE_TTL_MS = 60 * 60 * 1000L // 60 minutes
        private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    /**
     * Pre-warms the stream resolution in the background while the user is reading
     * details or looking at synopsis. When they click "Play", the stream is ready.
     */
    fun prewarmStream(contentType: String, contentId: String, contentTitle: String) {
        val cacheKey = "$contentType:$contentId"
        val cached = playbackCache[cacheKey]
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_TTL_MS) {
            return
        }

        if (activePrewarms.containsKey(cacheKey)) {
            return
        }

        val deferred = repoScope.async {
            try {
                val res = resolveStreamInternal(contentType, contentId, contentTitle)
                res.onSuccess { resolved ->
                    playbackCache[cacheKey] = CachedPlayback(resolved)
                }
                res
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                activePrewarms.remove(cacheKey)
            }
        }
        activePrewarms[cacheKey] = deferred
    }

    suspend fun resolveStream(
        contentType: String,
        contentId: String,
        contentTitle: String,
        onCountdown: ((Int) -> Unit)? = null
    ): Result<ResolvedPlayback> = withContext(Dispatchers.IO) {
        val cacheKey = "$contentType:$contentId"

        // 1. Instant hit: Already cached in memory
        val cached = playbackCache[cacheKey]
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_TTL_MS) {
            onCountdown?.invoke(0)
            return@withContext Result.success(cached.playback)
        }

        // 2. Pre-warming hit: Already resolving in background
        val ongoing = activePrewarms[cacheKey]
        if (ongoing != null) {
            try {
                val res = ongoing.await()
                if (res.isSuccess) {
                    onCountdown?.invoke(0)
                    return@withContext res
                }
            } catch (e: Exception) {}
        }

        // 3. Fallback to standard resolution
        val res = resolveStreamInternal(contentType, contentId, contentTitle, onCountdown)
        res.onSuccess { resolved ->
            playbackCache[cacheKey] = CachedPlayback(resolved)
        }
        res
    }

    private suspend fun resolveStreamInternal(
        contentType: String,
        contentId: String,
        contentTitle: String,
        onCountdown: ((Int) -> Unit)? = null
    ): Result<ResolvedPlayback> = withContext(Dispatchers.IO) {
        try {
            var targetType = contentType
            var targetId = contentId

            // If a TV series ID is passed instead of an episode ID, use episode 1 of season 1
            if (contentType == "tv_series") {
                val series = getSeriesDetail(contentId)
                val firstEp = series?.defaultSeason?.episodes?.firstOrNull()
                    ?: series?.seasons?.firstOrNull()?.let { s ->
                        getEpisodeDetail(series.slug, s.seasonNumber, 1)?.episode
                    }

                if (firstEp != null) {
                    targetType = "episode"
                    targetId = firstEp.id
                } else {
                    return@withContext Result.failure(Exception("No episodes available for this series."))
                }
            }

            onCountdown?.invoke(0)

            // One-shot backend resolver: TMDB metadata + VidNest stream resolution server-side
            val resolved = try {
                cinebyApi.resolvePlayback(
                    contentType = targetType,
                    contentId = targetId,
                    contentTitle = contentTitle
                )
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
            if (resolved.hlsMasterUrl.isBlank()) {
                return@withContext Result.failure(Exception("No playable stream found."))
            }
            Result.success(resolved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
