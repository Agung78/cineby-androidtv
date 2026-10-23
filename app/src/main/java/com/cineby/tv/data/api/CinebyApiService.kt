package com.cineby.tv.data.api

import com.cineby.tv.data.model.EpisodeResponse
import com.cineby.tv.data.model.GenreListResponse
import com.cineby.tv.data.model.MovieDetail
import com.cineby.tv.data.model.MovieListResponse
import com.cineby.tv.data.model.SearchResponse
import com.cineby.tv.data.model.SeriesDetail
import com.cineby.tv.data.model.SeriesListResponse
import com.cineby.tv.data.model.ResolvedPlayback
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CinebyApiService {

    @GET("api/movies")
    suspend fun getMovies(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("sort") sort: String? = null,
        @Query("genre") genre: String? = null
    ): MovieListResponse

    @GET("api/series")
    suspend fun getSeries(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("sort") sort: String? = null,
        @Query("genre") genre: String? = null
    ): SeriesListResponse

    @GET("api/movies/{slug}")
    suspend fun getMovieDetail(
        @Path("slug") slug: String
    ): MovieDetail

    @GET("api/series/{slug}")
    suspend fun getSeriesDetail(
        @Path("slug") slug: String
    ): SeriesDetail

    @GET("api/series/{slug}/season/{season}/episode/{episode}")
    suspend fun getEpisodeDetail(
        @Path("slug") slug: String,
        @Path("season") season: Int,
        @Path("episode") episode: Int
    ): EpisodeResponse

    @GET("api/genres")
    suspend fun getGenres(): GenreListResponse

    @GET("api/search")
    suspend fun search(
        @Query("q") query: String
    ): SearchResponse

    @GET("api/playback/resolve")
    suspend fun resolvePlayback(
        @Query("contentType") contentType: String,
        @Query("contentId") contentId: String,
        @Query("contentTitle") contentTitle: String? = null
    ): ResolvedPlayback
}
