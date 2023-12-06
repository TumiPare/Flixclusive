package com.flixclusive.data.repository

import androidx.compose.runtime.mutableStateListOf
import com.flixclusive.R
import com.flixclusive.common.LoggerUtils.errorLog
import com.flixclusive.data.utils.catchInternetRelatedException
import com.flixclusive.di.IoDispatcher
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.provider.SourceProviderDetails
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.domain.repository.FilmSourcesRepository
import com.flixclusive.domain.repository.TMDBRepository
import com.flixclusive.providers.models.common.MediaType
import com.flixclusive.providers.models.common.VideoData
import com.flixclusive.providers.sources.flixhq.FlixHQ
import com.flixclusive.providers.sources.superstream.SuperStream
import com.flixclusive.providers.utils.DecryptUtils.DecryptionException
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

class FilmSourcesRepositoryImpl @Inject constructor(
    client: OkHttpClient,
    private val tmdbRepository: TMDBRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FilmSourcesRepository {
    override val providers = mutableStateListOf(
        SourceProviderDetails(SuperStream(client)),
        SourceProviderDetails(FlixHQ(client)),
    )

    override suspend fun getSourceLinks(
        mediaId: String,
        providerIndex: Int,
        server: String?,
        season: Int?,
        episode: Int?,
    ): Resource<VideoData?> {
        return withContext(ioDispatcher) {
            try {
                return@withContext Resource.Success(
                    providers[providerIndex].source.getSourceLinks(
                        mediaId = mediaId,
                        server = server,
                        episode = episode,
                        season = season
                    )
                )
            } catch (e: Exception) {
                errorLog(e.stackTraceToString())
                return@withContext when (e) {
                    is DecryptionException -> Resource.Failure(R.string.decryption_error)
                    is JsonSyntaxException -> Resource.Failure(R.string.failed_to_extract)
                    else -> e.catchInternetRelatedException()
                }
            }
        }
    }

    override suspend fun getMediaId(
        film: Film?,
        providerIndex: Int,
    ): String? {
        return withContext(ioDispatcher) {
            try {
                val filmToSearch = if(film?.filmType == FilmType.TV_SHOW && film !is TvShow) {
                    val response = tmdbRepository.getTvShow(film.id)

                    if(response is Resource.Failure)
                        return@withContext null

                    response.data
                } else film

                if (filmToSearch == null)
                    return@withContext null

                var i = 1
                var id: String? = null
                val maxPage = 3

                while (id == null) {
                    if (i > maxPage) {
                        return@withContext ""
                    }

                    val searchResponse = providers[providerIndex].source.search(
                        query = filmToSearch.title,
                        page = i
                    )

                    if (searchResponse.results.isEmpty())
                        return@withContext ""


                    for(result in searchResponse.results) {
                        val titleMatches = result.title.equals(filmToSearch.title, ignoreCase = true)
                        val filmTypeMatches = result.mediaType?.type == filmToSearch.filmType.type
                        val releaseDateMatches =
                            result.releaseDate == filmToSearch.dateReleased.split(" ").last()

                        if (titleMatches && filmTypeMatches && filmToSearch is TvShow) {
                            if (filmToSearch.seasons.size == result.seasons) {
                                id = result.id
                                break
                            }

                            val tvShowInfo = providers[providerIndex].source.getMediaInfo(
                                mediaId = result.id!!,
                                mediaType = MediaType.TvShow
                            )

                            val tvReleaseDateMatches = tvShowInfo.releaseDate.split("-")
                                .first() == filmToSearch.dateReleased.split("-").first()
                            val seasonCountMatches = filmToSearch.seasons.size == tvShowInfo.seasons

                            if (tvReleaseDateMatches || seasonCountMatches) {
                                id = result.id
                                break
                            }
                        }

                        if (titleMatches && filmTypeMatches && releaseDateMatches) {
                            id = result.id
                            break
                        }
                    }

                    if (searchResponse.hasNextPage) {
                        i++
                    } else if (id.isNullOrEmpty()) {
                        id = ""
                        break
                    }
                }

                id
            } catch (e: Exception) {
                errorLog(e.stackTraceToString())
                null
            }
        }
    }
}