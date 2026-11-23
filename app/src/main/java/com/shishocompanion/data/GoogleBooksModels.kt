package com.shishocompanion.data

import kotlinx.serialization.Serializable

@Serializable
data class GoogleBooksResponse(
    val items: List<GoogleBookItem>? = null
)

@Serializable
data class GoogleBookItem(
    val volumeInfo: VolumeInfo? = null
)

@Serializable
data class VolumeInfo(
    val title: String? = null,
    val subtitle: String? = null,
    val authors: List<String>? = null,
    val publishedDate: String? = null,
    val description: String? = null,
    val industryIdentifiers: List<IndustryIdentifier>? = null,
    val imageLinks: ImageLinks? = null,
    val pageCount: Int? = null
)

@Serializable
data class OpenLibrarySearchResponse(
    val docs: List<OpenLibraryDoc>? = null
)

@Serializable
data class OpenLibraryDoc(
    val title: String? = null,
    val subtitle: String? = null,
    @kotlinx.serialization.SerialName("author_name") val authorName: List<String>? = null,
    @kotlinx.serialization.SerialName("first_publish_year") val firstPublishYear: Int? = null,
    val isbn: List<String>? = null,
    @kotlinx.serialization.SerialName("cover_i") val coverId: Long? = null,
    @kotlinx.serialization.SerialName("number_of_pages_median") val numberOfPagesMedian: Int? = null
)

@Serializable
data class IndustryIdentifier(
    val type: String? = null,
    val identifier: String? = null
)

@Serializable
data class ImageLinks(
    val thumbnail: String? = null,
    val smallThumbnail: String? = null
)

sealed interface BookSearchResult {
    data class Success(val books: List<VolumeInfo>) : BookSearchResult
    data object NoApiKey : BookSearchResult
    data class Error(val message: String? = null) : BookSearchResult
}
