package org.example

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.openapitools.client.apis.AnimeApi
import org.openapitools.client.apis.MangaApi
import org.openapitools.client.apis.SeasonsApi
import org.openapitools.client.apis.SeasonsApi.FilterGetSeason
import org.openapitools.client.infrastructure.ApiClient
import org.openapitools.client.models.Anime

val apiClient = ApiClient("https://api.jikan.moe/v4")
val animeApi = AnimeApi(apiClient.baseUrl, apiClient.client)
val seasonApi = SeasonsApi(apiClient.baseUrl, apiClient.client)
val mangaApi = MangaApi(apiClient.baseUrl, apiClient.client)
const val delayTime = 1000L

fun main() {
    runBlocking {
        getAllSeasonalAnime(2022, "fall")
            .mapNotNull { anime ->
                anime.malId?.let { malId ->
                    println("Fetching relations for animeId: $malId")
                    animeApi.getAnimeRelations(malId).also { delay(delayTime) }
                }?.data
                    ?.filter { it.relation?.lowercase() == "adaptation" }
                    ?.flatMap { it.entry.orEmpty() }
                    ?.filter { sourceMaterial -> sourceMaterial.type == "manga" }
                    ?.ifEmpty {
                        println("No valid adaptation found for ${anime.malId}")
                        null
                    }
                    ?.mapNotNull { sourceMaterial -> sourceMaterial.malId }
                    ?.map { sourceMaterialIds ->
                        println("Fetching manga for: ${anime.malId}")
                        mangaApi.getMangaById(sourceMaterialIds).also { delay(delayTime) }.data
                    }
                    ?.maxByOrNull { manga -> manga?.score ?: 0f }
                    ?.let { manga ->
                        AnimeByAdaptationRating(
                            animeTitle = anime.title,
                            sourceMaterialRating = manga.score
                        )
                    }
            }
    }.let { result ->
        result.sortedByDescending { it.sourceMaterialRating }
    }.forEach {
        println("${it.animeTitle}:   ${it.sourceMaterialRating ?: "N/A"}")
    }
}

suspend fun getAllSeasonalAnime(year: Int, season: String): List<Anime> {
    val allAnime = mutableListOf<Anime>()
    var currentPage = 1
    var hasNextPage = true

    while (hasNextPage) {
        println("Fetching page $currentPage for $season $year...")
        seasonApi.getSeason(
            year = year,
            season = season,
            page = currentPage,
            limit = 25,
            filter = FilterGetSeason.TV
        ).also { response ->
            delay(delayTime)
            response.data?.let { allAnime.addAll(it) }
            hasNextPage = response.pagination?.hasNextPage == true
            currentPage++
        }
    }
    println("Found ${allAnime.size} anime")

    return allAnime.distinctBy { it.malId }
}

data class AnimeByAdaptationRating(
    val animeTitle: String?,
    val sourceMaterialRating: Float?
)