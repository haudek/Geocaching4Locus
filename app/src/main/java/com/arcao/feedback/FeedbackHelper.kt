package com.arcao.feedback

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.annotation.NonNull
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import com.arcao.feedback.collector.Collector
import com.arcao.geocaching4locus.App
import com.arcao.geocaching4locus.base.coroutine.CoroutinesDispatcherProvider
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.Stack
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FeedbackHelper(
    private val app: App,
    private val collectors: Array<Collector>,
    private val dispatcherProvider: CoroutinesDispatcherProvider
) {
    private val context: Context = app

    suspend fun createFeedbackIntent(@StringRes resEmail: Int, @StringRes resSubject: Int, @StringRes resMessageText: Int): Intent =
        coroutineScope {
            withContext(dispatcherProvider.computation) {
                val email = context.getString(resEmail)
                val subject = context.getString(resSubject, app.name, app.version)
                val message = context.getString(resMessageText)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    // only e-mail apps
                    type = "plain/text"
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, message)
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    val reportFile = getReportFile(context)
                    val reportUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", reportFile)

                    createReport(reportFile)

                    intent.putExtra(Intent.EXTRA_STREAM, reportUri)
                    intent.clipData = ClipData.newRawUri("", reportUri)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: IOException) {
                    Timber.e(e)
                }

                createEmailOnlyChooserIntent(intent)
            }
        }

    @Throws(IOException::class)
    private suspend fun createReport(reportFile: File) {
        reportFile.parentFile.mkdirs()

        if (reportFile.exists()) {
            Timber.d("Report file $reportFile already exist.")
            if (reportFile.delete()) {
                Timber.d("Report file removed.")
            }
        }

        Timber.d("Creating report to %s", reportFile)
        ZipOutputStream(FileOutputStream(reportFile)).use { zos -> writeCollectors(zos) }
        Timber.d("Report created.")
    }

    @Throws(IOException::class)
    private suspend fun writeCollectors(zos: ZipOutputStream) {
        for (collector in collectors) {
            zos.putNextEntry(ZipEntry(collector.name + ".txt"))

            OutputStreamWriter(zos, "UTF-8").apply {
                write(
                    try {
                        collector()
                    } catch (ignored: Exception) {
                        // ignored
                        ""
                    }
                )
                flush()
            }

            zos.closeEntry()
        }
    }

    private fun createEmailOnlyChooserIntent(source: Intent, chooserTitle: CharSequence? = null): Intent {
        val intents = Stack<Intent>()
        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:info@domain.com"))

        context.packageManager.queryIntentActivities(emailIntent, 0).forEach { resolveInfo ->
            intents.add(Intent(source).setPackage(resolveInfo.activityInfo.packageName))
        }

        return if (!intents.isEmpty()) {
            Intent.createChooser(intents.removeAt(0), chooserTitle)
                .putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray<Parcelable>())
        } else {
            // fall back when no e-mail app is installed
            source.type = "*/*"
            Intent.createChooser(source, chooserTitle)
        }
    }

    companion object {
        private const val REPORT_FILE_NAME = "logs.zip"

        fun getReportFile(@NonNull context: Context): File {
            return File(File(context.cacheDir, "export"), REPORT_FILE_NAME)
        }
    }
}
