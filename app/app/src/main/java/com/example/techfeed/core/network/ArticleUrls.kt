package com.example.techfeed.core.network

import java.net.URI

fun safeArticleUrl(value: String): String? = runCatching {
    URI(value.trim())
        .takeIf {
            it.scheme in listOf("https", "http") && !it.host.isNullOrBlank() && it.userInfo == null
        }
        ?.toASCIIString()
}
    .getOrNull()

fun safeFeedUrl(value: String): String? = runCatching {
    URI(value.trim())
        .takeIf { it.scheme == "https" && !it.host.isNullOrBlank() && it.userInfo == null }
        ?.toASCIIString()
}
    .getOrNull()
