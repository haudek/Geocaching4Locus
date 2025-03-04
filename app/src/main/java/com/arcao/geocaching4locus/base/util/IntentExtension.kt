package com.arcao.geocaching4locus.base.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast

fun Activity.showWebPage(uri: Uri): Boolean {
    @Suppress("DEPRECATION")
    val intent = Intent(Intent.ACTION_VIEW, uri)
        .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

    return if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
        true
    } else {
        Toast.makeText(this, "Web page cannot be opened. No application found to show web pages.", Toast.LENGTH_LONG)
            .show()
        false
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Intent.isCallableWith(context: Context): Boolean {
    return context.packageManager.queryIntentActivities(this, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
}