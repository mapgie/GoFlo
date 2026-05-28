package com.mapgie.goflo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.mapgie.goflo.GoFloApplication
import com.mapgie.goflo.MainActivity
import com.mapgie.goflo.R
import com.mapgie.goflo.data.database.entities.TrackingCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 2×2 home-screen widget showing up to four active tracking categories as
 * tappable buttons. Tapping a button opens the app's log screen for that
 * category (today's date). Tapping the widget header opens the app home screen.
 *
 * Category list is read from [TrackingRepository] on [Dispatchers.IO] and is
 * refreshed every 30 minutes by the OS plus whenever [updateAllWidgets] is called.
 */
class QuickLogWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { widgetId ->
            val pendingResult = goAsync()
            widgetScope.launch {
                try {
                    pushUpdate(context, appWidgetManager, widgetId)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {

        /** Intent extra key for the category ID — read by MainActivity to navigate directly. */
        const val EXTRA_CATEGORY_ID = "com.mapgie.goflo.EXTRA_CATEGORY_ID"

        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, QuickLogWidget::class.java))
            if (ids.isEmpty()) return
            widgetScope.launch {
                ids.forEach { id -> pushUpdate(context, manager, id) }
            }
        }

        // ── Private ──────────────────────────────────────────────────────────

        private suspend fun pushUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
        ) {
            val app = context.applicationContext as GoFloApplication
            val categories = app.trackingRepository
                .getActiveCategories()
                .first()
                .filter { !it.isSystem && !it.isArchived }
                .sortedBy { it.displayOrder }
                .take(4)

            val views = RemoteViews(context.packageName, R.layout.widget_quick_log)

            // Tap on the header / root opens the app home screen
            val openApp = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.ql_label, openApp)

            val slots = listOf(
                Triple(R.id.ql_cat_1, R.id.ql_name_1, R.id.ql_row1),
                Triple(R.id.ql_cat_2, R.id.ql_name_2, R.id.ql_row1),
                Triple(R.id.ql_cat_3, R.id.ql_name_3, R.id.ql_row2),
                Triple(R.id.ql_cat_4, R.id.ql_name_4, R.id.ql_row2),
            )

            if (categories.isEmpty()) {
                views.setViewVisibility(R.id.ql_row1, View.GONE)
                views.setViewVisibility(R.id.ql_row2, View.GONE)
                views.setViewVisibility(R.id.ql_empty, View.VISIBLE)
                views.setOnClickPendingIntent(R.id.ql_empty, openApp)
            } else {
                views.setViewVisibility(R.id.ql_empty, View.GONE)
                views.setViewVisibility(R.id.ql_row1, View.VISIBLE)
                views.setViewVisibility(
                    R.id.ql_row2,
                    if (categories.size > 2) View.VISIBLE else View.GONE
                )

                slots.forEachIndexed { index, (containerId, nameId, _) ->
                    val category = categories.getOrNull(index)
                    if (category != null) {
                        views.setViewVisibility(containerId, View.VISIBLE)
                        views.setTextViewText(nameId, category.name)
                        views.setOnClickPendingIntent(containerId, buildCategoryIntent(context, category))
                    } else {
                        views.setViewVisibility(containerId, View.GONE)
                    }
                }
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun buildCategoryIntent(context: Context, category: TrackingCategory): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_CATEGORY_ID, category.id)
            }
            // Use category.id as request code so each category gets a distinct PendingIntent.
            return PendingIntent.getActivity(
                context,
                category.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
