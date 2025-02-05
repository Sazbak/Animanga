package org.example

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.example.common.capitalizeFirstLetter
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
const val animeLogTemplate = "Anime Title: %s, Mal Id: %d"

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Provide parameters: <year> <season>")
        return
    }

    runBlocking {
        getAllSeasonalAnime(args[0].toInt(), args[1])
            .mapNotNull { anime ->
                anime.malId?.let { malId ->
                    println("Fetching relations for " + String.format(animeLogTemplate, anime.title, anime.malId))
                    animeApi.getAnimeRelations(malId).also { delay(delayTime) }
                }?.data
                    ?.filter { it.relation?.lowercase() == "adaptation" }
                    ?.flatMap { it.entry.orEmpty() }
                    ?.filter { sourceMaterial -> sourceMaterial.type == "manga" }
                    ?.ifEmpty {
                        println(
                            "No valid adaptation found for " + String.format(
                                animeLogTemplate,
                                anime.title,
                                anime.malId
                            )
                        )
                        null
                    }
                    ?.mapNotNull { sourceMaterial -> sourceMaterial.malId }
                    ?.map { sourceMaterialIds ->
                        println("Fetching manga for " + String.format(animeLogTemplate, anime.title, anime.malId))
                        mangaApi.getMangaById(sourceMaterialIds).also { delay(delayTime) }.data
                    }
                    ?.maxByOrNull { manga -> manga?.score ?: 0f }
                    ?.let { manga ->
                        AnimeByAdaptationRating(
                            title = anime.title,
                            sourceMaterialRating = manga.score,
                            malUrl = anime.url
                        )
                    }
            }
    }.let { result ->
        println("")
        println("${args[0]} ${args[1].capitalizeFirstLetter()}")
        println("--------------------")
        result.sortedByDescending { it.sourceMaterialRating }
    }.forEach {
        println("${it.title}:   ${it.sourceMaterialRating ?: "N/A"}")
        println("${it.malUrl}")
        println("")
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
    val title: String?,
    val malUrl: String?,
    val sourceMaterialRating: Float?
)