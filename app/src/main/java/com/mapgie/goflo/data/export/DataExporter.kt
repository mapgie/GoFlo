package com.mapgie.goflo.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDate

/**
 * Writes a JSON export string to a temporary cache file and returns an
 * ACTION_SEND intent pointing at it via FileProvider, ready to pass to
 * Context.startActivity(Intent.createChooser(...)).
 *
 * The cache file lives in context.cacheDir/exports/. Because the file name
 * embeds the date, every export first clears the directory so full health
 * exports never accumulate in the cache; "Delete All Data" clears it too
 * via [clearExportCache].
 */
object DataExporter {

    /** Removes every previously written export file from the cache. */
    fun clearExportCache(context: Context) {
        File(context.cacheDir, "exports").deleteRecursively()
    }

    private fun freshExportDir(context: Context): File {
        clearExportCache(context)
        return File(context.cacheDir, "exports").also { it.mkdirs() }
    }

    fun buildShareIntent(context: Context, json: String): Intent {
        val dir = freshExportDir(context)
        val file = File(dir, "goflo_export_${LocalDate.now()}.json")
        file.writeText(json, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "GoFlo data export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Export GoFlo data")
    }

    /**
     * Writes [text] to a temporary cache file and returns an ACTION_SEND intent
     * for sharing a plain-text doctor visit summary.
     */
    fun buildTextShareIntent(context: Context, text: String): Intent {
        val dir  = freshExportDir(context)
        val file = File(dir, "goflo_cycle_summary_${LocalDate.now()}.txt")
        file.writeText(text, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Cycle summary for healthcare provider")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Share cycle summary")
    }

    /**
     * Writes [csv] to a temporary cache file and returns an ACTION_SEND intent
     * pointing at it via FileProvider, ready for Context.startActivity().
     */
    fun buildCsvShareIntent(context: Context, csv: String): Intent {
        val dir  = freshExportDir(context)
        val file = File(dir, "goflo_export_${LocalDate.now()}.csv")
        file.writeText(csv, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "GoFlo data export (CSV)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Export GoFlo data as CSV")
    }
}
