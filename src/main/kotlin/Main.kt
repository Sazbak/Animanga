package org.example

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.example.common.capitalizeFirstLetter
import org.openapitools.client.apis.AnimeApi
import org.openapitools.client.apis.MangaApi
import org.openapitools.client.apis.SeasonsApi
import org.openapitools.client.infrastructure.ApiClient
import org.openapitools.client.models.Anime
import org.openapitools.client.models.Manga

val apiClient = ApiClient("https://api.jikan.moe/v4")
val animeApi = AnimeApi(apiClient.baseUrl, apiClient.client)
val seasonApi = SeasonsApi(apiClient.baseUrl, apiClient.client)
val mangaApi = MangaApi(apiClient.baseUrl, apiClient.client)
const val delayTime = 1000L
const val animeLogTemplate = "Anime Title: %s, Mal Id: %d"

fun main(args: Array<String>) {
    Params().main(args)
}

fun start(year: Int, season: String, hasNoPrequel: Boolean, hasNoSequel: Boolean) {
    runBlocking {
        getAllSeasonalAnime(year, season)
            .mapNotNull { anime ->
                anime.malId?.let { malId ->
                    println("Fetching relations for " + String.format(animeLogTemplate, anime.title, anime.malId))
                    animeApi.getAnimeRelations(malId).also { delay(delayTime) }
                }?.data
                    ?.takeUnless { relations -> hasNoPrequel && relations.any { it.relation?.lowercase() == "prequel" } }
                    ?.takeUnless { relations -> hasNoSequel && relations.any { it.relation?.lowercase() == "sequel" } }
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
                    ?.mapNotNull { sourceMaterialIds ->
                        println("Fetching manga for " + String.format(animeLogTemplate, anime.title, anime.malId))
                        mangaApi.getMangaById(sourceMaterialIds).also { delay(delayTime) }.data
                    }
                    ?.let { adaptationList ->
                        adaptationList
                            .filter { it.type == Manga.Type.MANGA }
                            .maxByOrNull { it.score ?: 0f }
                            ?: adaptationList.maxByOrNull { it.score ?: 0f }
                    }
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
        println("$year ${season.capitalizeFirstLetter()}")
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
            filter = SeasonsApi.FilterGetSeason.TV
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

enum class SEASON { WINTER, SPRING, SUMMER, FALL }

class Params : CliktCommand() {
    private val year by argument().int()
    private val season by argument().enum<SEASON>()
    private val hasNoPrequel by option("-hnp", "--has-no-prequel").flag()
    private val hasNoSequel by option("-hns", "--has-no-sequel").flag()

    override fun run() {
        start(year = year, season = season.name, hasNoPrequel = hasNoPrequel, hasNoSequel = hasNoSequel)
    }
}