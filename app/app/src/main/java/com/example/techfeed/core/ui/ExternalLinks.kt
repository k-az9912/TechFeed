package com.example.techfeed.core.ui

import com.example.techfeed.core.network.safeArticleUrl
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Returns false if the URL is invalid or no browser can open it. */
fun openExternalLink(context: Context, url: String): Boolean {
    val safeUrl = safeArticleUrl(url) ?: return false
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
