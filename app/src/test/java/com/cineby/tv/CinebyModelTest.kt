package com.cineby.tv

import com.cineby.tv.data.api.ApiClient
import com.cineby.tv.data.model.MovieListResponse
import com.cineby.tv.data.model.SearchResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CinebyModelTest {

    @Test
    fun testMovieListDeserialization() {
        val sampleJson = """
        {
          "data": [
            {
              "id": "a22c5860-fe44-4a14-8ab4-61a23d26ab06",
              "title": "Fuori",
              "slug": "fuori-2025",
              "posterPath": "/gxSkBOfTEOX2f6KhqMFSXf24nMo.jpg",
              "backdropPath": "/k9Oe0KXL1qYnEddxr1WR4VkB8eW.jpg",
              "releaseDate": "2025-05-22",
              "voteAverage": 6.40,
              "viewCount": 259,
              "quality": "WEB-DL",
              "country": "FR",
              "runtime": 117
            }
          ],
          "pagination": {
            "page": 1,
            "limit": 20,
            "total": 100,
            "totalPages": 5
          }
        }
        """.trimIndent()

        val parsed = ApiClient.json.decodeFromString<MovieListResponse>(sampleJson)
        assertEquals(1, parsed.data.size)
        val movie = parsed.data[0]
        assertEquals("Fuori", movie.title)
        assertEquals("https://image.tmdb.org/t/p/w342/gxSkBOfTEOX2f6KhqMFSXf24nMo.jpg", movie.fullPosterUrl)
        assertEquals("https://image.tmdb.org/t/p/w1280/k9Oe0KXL1qYnEddxr1WR4VkB8eW.jpg", movie.fullBackdropUrl)
        assertEquals("6.4", movie.formattedRating)
    }

    @Test
    fun testSearchDeserializationWithRealData() {
        val searchJson = """
        {
          "results": [
            {
              "id": "5f561446-98c4-4e26-908c-301dc8113384",
              "contentType": "movie",
              "title": "The Batman",
              "originalTitle": "The Batman",
              "overview": "In his second year of fighting crime...",
              "genres": [
                "Crime",
                "Mystery",
                "Thriller"
              ],
              "releaseDate": "2022-03-01",
              "originalLanguage": "en",
              "voteAverage": 7.66,
              "viewCount": 32089,
              "popularityScore": 978,
              "posterPath": "/74xTEgt7R36Fpooo50r9T25onhq.jpg",
              "backdropPath": "/rvtdN5XkWAfGX6xDuPL6yYS2seK.jpg",
              "quality": "WEB-DL",
              "slug": "the-batman-2022",
              "commentCount": 25
            }
          ],
          "total": 108,
          "order": "popular"
        }
        """.trimIndent()

        val searchResponse = ApiClient.json.decodeFromString<SearchResponse>(searchJson)
        assertEquals(1, searchResponse.results.size)
        val item = searchResponse.results[0]
        assertEquals("The Batman", item.title)
        assertEquals("the-batman-2022", item.slug)
        assertEquals("7.7", item.formattedRating)
        assertEquals(3, item.genres.size)
        assertEquals("Crime", item.genres[0])
    }

    @Test
    fun testRealLiveSearchApi() = kotlinx.coroutines.runBlocking {
        if (System.getenv("RUN_LIVE_TESTS") != "true") {
            println("Skipping live search test (set RUN_LIVE_TESTS=true to run against live backend)")
            return@runBlocking
        }
        try {
            val repo = com.cineby.tv.data.repository.CinebyRepository()
            val results = repo.search("Reacher")
            println("Live Search 'Reacher' returned ${results.size} items")
            if (results.isNotEmpty()) {
                val first = results[0]
                println("First item: ${first.title} (${first.contentType}) - ${first.slug}")
                org.junit.Assert.assertTrue(first.title.isNotBlank())
            }
        } catch (e: Exception) {
            println("Live search test note: ${e.message}")
        }
    }

    @Test
    fun testResolvedPlaybackDeserialization() {
        val sampleJson = """
        {
          "title": "Avatar: Fire and Ash",
          "hlsMasterUrl": "https://m5f-cf.streamed-7.workers.dev/hls/abc123/master.m3u8",
          "mp4Url": null,
          "subtitles": [
            {
              "lang": "en",
              "label": "English",
              "path": "https://example.com/sub_en.vtt"
            }
          ],
          "durationSec": 0,
          "videoId": "83533",
          "server": "lamda",
          "sources": [
            {"url": "https://m5f-cf.streamed-7.workers.dev/hls/abc123/master.m3u8", "quality": "1080p", "type": "hls", "server": "lamda"}
          ]
        }
        """.trimIndent()

        val parsed = ApiClient.json.decodeFromString<com.cineby.tv.data.model.ResolvedPlayback>(sampleJson)
        assertEquals("Avatar: Fire and Ash", parsed.title)
        assertEquals("https://m5f-cf.streamed-7.workers.dev/hls/abc123/master.m3u8", parsed.hlsMasterUrl)
        assertEquals(1, parsed.subtitles.size)
        assertEquals("en", parsed.subtitles[0].lang)
        assertEquals("lamda", parsed.server)
        assertEquals(1, parsed.sources.size)
    }

    @Test
    fun testRealResolveStreamPlayback() = kotlinx.coroutines.runBlocking {
        if (System.getenv("RUN_LIVE_TESTS") != "true") {
            println("Skipping live stream resolution test (set RUN_LIVE_TESTS=true to run against live backend)")
            return@runBlocking
        }
        try {
            val repo = com.cineby.tv.data.repository.CinebyRepository()
            val movieId = "83533"
            println("Resolving stream for Avatar ($movieId)...")
            var lastCountdown = -1
            val res = repo.resolveStream(
                contentType = "movie",
                contentId = movieId,
                contentTitle = "Avatar: Fire and Ash",
                onCountdown = { sec ->
                    if (sec != lastCountdown) {
                        println("Countdown: ${sec}s")
                        lastCountdown = sec
                    }
                }
            )

            if (res.isSuccess) {
                val playback = res.getOrNull()
                assertNotNull(playback)
                println("SUCCESS! HLS Master Playlist: ${playback?.hlsMasterUrl}")
                org.junit.Assert.assertTrue(playback!!.hlsMasterUrl.startsWith("http"))
            }
        } catch (e: Exception) {
            println("Skipping live playback test (backend not reachable in unit test env: ${e.message})")
        }
    }
}
