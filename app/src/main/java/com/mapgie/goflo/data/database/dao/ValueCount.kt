package com.mapgie.goflo.data.database.dao

/**
 * Room projection for aggregate GROUP BY queries on tracking_log_values.
 *
 * [valueLabel] matches the `valueLabel` column in tracking_log_values.
 * [count]      is the SQL `COUNT(*)` alias returned by the query.
 */
data class ValueCount(val valueLabel: String, val count: Int)
