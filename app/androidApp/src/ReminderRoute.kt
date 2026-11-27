package com.ygoular.keepit

import android.net.Uri

internal object ReminderRoute {
    fun uri(id: String): Uri = Uri.Builder().scheme("keepit").authority("note").appendPath(id).build()

    fun noteId(uri: Uri?): String? = uri
        ?.takeIf { it.scheme == "keepit" && it.authority == "note" && it.pathSegments.size == 1 }
        ?.lastPathSegment
}
