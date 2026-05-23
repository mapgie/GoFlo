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
 * The cache file lives in context.cacheDir/exports/ and is overwritten on
 * every export — no accumulation of old files.
 */
object DataExporter {

    fun buildShareIntent(context: Context, json: String): Intent {
        val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
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
     * Writes [csv] to a temporary cache file and returns an ACTION_SEND intent
     * pointing at it via FileProvider, ready for Context.startActivity().
     */
    fun buildCsvShareIntent(context: Context, csv: String): Intent {
        val dir  = File(context.cacheDir, "exports").also { it.mkdirs() }
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
