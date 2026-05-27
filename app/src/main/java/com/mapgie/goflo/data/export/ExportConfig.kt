package com.mapgie.goflo.data.export

import java.time.LocalDate

enum class ExportFormat { JSON, CSV }

enum class DateRangePreset(val label: String) {
    ALL_TIME("All time"),
    LAST_3_MONTHS("Last 3 months"),
    LAST_6_MONTHS("Last 6 months"),
    LAST_YEAR("Last year"),
    CUSTOM("Custom")
}

data class ExportConfig(
    val format: ExportFormat = ExportFormat.JSON,
    val includePeriods: Boolean = true,
    val selectedCategoryIds: Set<Long> = emptySet(),
    val dateRangePreset: DateRangePreset = DateRangePreset.ALL_TIME,
    val customStartDate: LocalDate? = null,
    val customEndDate: LocalDate? = null
) {
    /** Resolved start/end dates, or null pair for "all time". */
    val effectiveDateRange: Pair<LocalDate?, LocalDate?>
        get() = when (dateRangePreset) {
            DateRangePreset.ALL_TIME      -> null to null
            DateRangePreset.LAST_3_MONTHS -> LocalDate.now().minusMonths(3) to LocalDate.now()
            DateRangePreset.LAST_6_MONTHS -> LocalDate.now().minusMonths(6) to LocalDate.now()
            DateRangePreset.LAST_YEAR     -> LocalDate.now().minusYears(1) to LocalDate.now()
            DateRangePreset.CUSTOM        -> customStartDate to customEndDate
        }
}
