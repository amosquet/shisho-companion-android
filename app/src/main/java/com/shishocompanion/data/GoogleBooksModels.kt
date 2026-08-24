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
    val imageLinks: ImageLinks? = null
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
