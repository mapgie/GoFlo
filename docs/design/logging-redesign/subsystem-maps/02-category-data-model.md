# Subsystem map: Category / tracking data model & DB layer

> **Mapped against**
> - Commit: `d07d947` (`d07d947f5b2463eaa08e6521d3228026c55b2bef`)
> - versionCode **116**, versionName **0.53.0-beta.1**, DB schema version **23**
> - Date: 2026-08-22
>
> **Staleness check for future sessions:** the DB class is `data/database/GoFloDatabase.kt`. Confirm its `version = N` before writing a migration — if it is no longer **23**, someone added migrations after this map; read them and target `N → N+1`. Run `git diff d07d947 -- app/src/main/java/com/mapgie/goflo/data/` to see drift.

All paths under `app/src/main/java/com/mapgie/goflo/`. **The DB class is `GoFloDatabase.kt`, not `AppDatabase.kt`.**

## 1. Entity schemas

### TrackingCategory — table `tracking_categories` (`entities/TrackingCategory.kt`)
The central category entity. **Note how much already exists** — icons, colour token, input type, allow-multiple, log-with-period, and time-tracking are all present today.

| Field | Type | Default | Notes |
|---|---|---|---|
| `id` | `Long` | `0` autoGen PK | |
| `name` | `String` | — | user-visible name |
| `isSystem` | `Boolean` | `false` | Flow/Symptoms seeded true; can't delete (UI-enforced) |
| `systemKey` | `String` | `""` | stable key: `"flow"`, `"symptoms"`, else `""` |
| `displayOrder` | `Int` | `0` | |
| `iconName` | `String` | `"category"` | → `CategoryIcon.key` (20 curated icons) |
| `colorToken` | `String` | `"secondary"` | semantic token OR 8-char AARRGGBB hex |
| `categoryType` | `String` | `"default"` | input-type discriminator; **immutable after creation (current rule)** |
| `numericMin` | `Float` | `0f` | |
| `numericMax` | `Float` | `10f` | |
| `allowDecimals` | `Boolean` | `false` | |
| `numericUnit` | `String` | `""` | suffix e.g. "°C" |
| `scaleLabels` | `String` | `""` | newline `value=label` pairs, slider only |
| `isArchived` | `Boolean` | `false` | hides from logging UI |
| `allowMultiple` | `Boolean` | `false` | multiple logs per day |
| `showInLogPeriod` | `Boolean` | `false` | pin to Log Period screen |
| `trackAgainstTime` | `Boolean` | `false` | enables `loggedAt` time on logs |
| `modeKey` | `String` | `""` | links to a tracking-mode preset |

Computed (not a column): `val isNumeric get() = categoryType != "default"`.

### TrackingValue — table `tracking_values`
Catalog of selectable options for a category (this is how "Spot/Light/Med/Heavy" are stored).
- `id: Long` PK, `categoryId: Long` (FK → categories, `ON DELETE CASCADE`, indexed), `label: String`, `displayOrder: Int = 0`, `isSeeded: Boolean = false` (protects shipped values from deletion).

### TrackingLog — table `tracking_logs`
Per-(date,category) entry (one row per pair, unless `allowMultiple`).
- `id: Long` PK, `date: String` (ISO), `categoryId: Long` (FK cascade; indices on `categoryId` and `date`), `notes: String = ""`, `loggedAt: String = ""` (HH:mm; empty when not time-tracked).

### TrackingLogValue — table `tracking_log_values`
Selected values on a log — a **string snapshot**, NOT an FK to `TrackingValue` (so renames/deletes don't corrupt history).
- `id: Long` PK, `logId: Long` (FK → logs cascade, indexed), `valueLabel: String`.

### ColorProfile — table `color_profiles`
Saved custom **app-theme** palette slots (unrelated to per-category colour).
- `id, name="", primaryArgb=0, secondaryArgb=0, tertiaryArgb=0, lightBackgroundArgb=0, darkBackgroundArgb=0` (0 = Auto).

### PeriodEntry — table `periods`
Derived period *episode*. `id, startDate, endDate: String? = null, flowLevel="Medium", notes=""`.

### PeriodDayEntry — table `period_days`
Per-day source of truth; `date` unique-indexed. `id, date`.

### SymptomEntry — table `symptoms`
Legacy per-period symptom, FK → `PeriodEntry` cascade. `id, periodId, symptomType` (stored as display label after MIGRATION_14_15).

## 2. Database config — `data/database/GoFloDatabase.kt`

- **Current version: 23**, `exportSchema = false`.
- **@Database entities:** `PeriodEntry, PeriodDayEntry, SymptomEntry, TrackingCategory, TrackingValue, TrackingLog, TrackingLogValue, CustomAlarm, CustomAlarmCategory, ColorProfile`.
- Fresh installs seed via `onCreate → seedSystemCategories`: Flow (icon `water`, token `primary`, values Spotting/Light/Medium/Heavy) and Symptoms (icon `healing`, token `tertiary`, values Cramps/Headache/Bloating/Fatigue/Back Pain/Mood Swings/Bleeding (non-period)). Values seeded with `isSeeded=1`.
- `PRAGMA foreign_keys = ON` re-applied on every open.

### Migration chain (all registered in `.addMigrations(...)`, chain 1→23)
1_2 create `custom_symptoms` (later dropped) · 2_3 create the 4 tracking tables + seed Flow/Symptoms · 3_4 add `iconName`+`colorArgb` · 4_5 rebuild → replace `colorArgb` with `colorToken` · 5_6 add `isNumeric,numericMin,numericMax,allowDecimals` · 6_7 rebuild → replace `isNumeric` with `categoryType`, add `numericUnit,isArchived` · 7_8 add `allowMultiple` · 8_9 add `showInLogPeriod` · 9_10 add `scaleLabels` · 10_11 add `systemKey` · 11_12 add `trackAgainstTime`+`loggedAt` · 12_13 insert "Bleeding (non-period)" · 13_14 add `isSeeded` · 14_15 convert enum names→labels, un-seed flow/symptom values, migrate custom_symptoms→values, drop custom_symptoms · 15_16 seed "Ovulation Test" · 16_17 add `modeKey` · 17_18 create `custom_alarms`+`custom_alarm_categories` · 18_19 demote Ovulation Test to non-system, `modeKey='ovulation_test'` · 19_20 `showInLogPeriod=1` for flow/symptoms · 20_21 create `color_profiles` · 21_22 add light/dark background argb · 22_23 create `period_days` (unique date), backfill from episode ranges.

**Pattern to follow:** additive column adds use `ALTER TABLE ... ADD COLUMN`; type changes do a full table rebuild (create-new, copy, drop, rename). Never `fallbackToDestructiveMigration` (forbidden by CLAUDE.md).

## 3. DAOs

### TrackingCategoryDao
Categories: `getAllCategories()`/`getActiveCategories()` (isArchived=0) Flows; `getCategoryById`/`…Once`; `getAllCategoriesOnce`; lookups `getSystemCategoryByName/ByKey`, `getCategoryByName`, `getCategoryByModeKey`; `getShowInLogPeriodCategoriesOnce`. Mutations: `insertCategory` (REPLACE→Long), `updateCategory`, `deleteCategory`, `deleteAllCustomCategories`, `unarchiveAllSystemCategories`.
Values: `getValuesForCategory`/`…Once`, `insertValue` (IGNORE), `updateValue`, `deleteValue`, `bulkRenameLogValues(categoryId, oldLabel, newLabel)`.

### TrackingLogDao
Logs: `getLogsForDate`/`…Once`; `getAllLogDates`; `getLogById`/`…Once`; `getLogForDateAndCategory` (LIMIT 1); `getLogsForDateAndCategory` (multiple, ordered by loggedAt); `insertLog` (REPLACE→Long), `updateLog`, `deleteLog`. Log values: `getLogValuesForLog`/`…Once`, `insertLogValue`, `deleteLogValuesForLog`. Stats/export: `getLogsForCategoryInRange`, `getValueCountsForCategory` (→ `ValueCount`), `getAllLogsInRange`, `getLogsForCategoriesInRange`, `getAllLogsForCategories`, `getLogValuesForLogs`, `getEarliest/LatestLogDate`, delete ranges/date/all.

## 4. Repository — `data/repository/TrackingRepository.kt`
Constructor: `TrackingRepository(categoryDao, logDao, symptomDao?)`. Wrapper `TrackingLogWithValues(log, category, values: List<String>)`.

Category CRUD: `getAllCategories()/getActiveCategories(): Flow<List<TrackingCategory>>`, `getAllCategoriesOnce()`, `getShowInLogPeriodCategoriesOnce()`, `getCategoryById(id)/…Once`, `getValuesForCategory(id)/…Once`, `addCategory(name, iconName, colorToken, categoryType, numericMin, numericMax, allowDecimals, numericUnit, scaleLabels, allowMultiple, showInLogPeriod, trackAgainstTime, modeKey): Long`, `renameCategory`, `updateCategoryAppearance(id, iconName, colorToken)`, `updateCategoryFullSettings(...)`, `updateTrackAgainstTime`, `updateNumericSettings`, `updateNumericUnit`, `updateShowInLogPeriod`, `updateAllowMultiple`, `updateFlowCategoryMode(id, useSlider)`, `archiveCategory`, `unarchiveCategory`, `deleteCategory` (guards `isSystem`), `reorderCategories`, `getExistingModeKeys`. Values: `addValueToCategory`, `deleteValue`, `renameValue(value, newLabel, fixHistorical)`.

Log CRUD: `getLogsForDate(date): Flow<List<TrackingLogWithValues>>`, `getAllLogDates(): Flow<Set<LocalDate>>`, `saveLog(date, categoryId, selectedValues, notes, allowMultiple=false, loggedAt=""): Long` (upsert unless allowMultiple; deletes+re-inserts log values each time), `updateLogInPlace(...)`, `deleteLog`, `incrementLog(date, categoryId, delta=1): Int`, `getExistingLog`, `getLogsForDateAndCategory`, `getLogById`, stats/export accessors, `deleteAllLogs`, `resetCategoryConfiguration`, `deleteLogsForPeriod`, `syncFlowLogsForPeriod`.

## 5. Answers to the model questions that matter for the redesign

- **Category colour** = single `TrackingCategory.colorToken: String` (default `"secondary"`). Holds *either* a semantic token (`primary`/`secondary`/`tertiary`) *or* an 8-char AARRGGBB hex. Resolution in `ui/util/CategoryAppearance.kt`. **There is no "role" concept beyond these 3 tokens, and no group.** → The handover's `FIXED(argb)` role maps directly onto the existing hex path. The handover's `QUATERNARY/QUINARY/SENARY` are new tokens to add.
- **Selectable values** = `TrackingValue` rows (ordered, `isSeeded`). Numeric/slider categories use `numericMin/Max/allowDecimals/numericUnit/scaleLabels` instead. Logged value is snapshotted as `TrackingLogValue.valueLabel`.
- **Input type** already exists as `categoryType` (`default`/`numeric_slider`/`numeric_free`/`increment`), enum `CategoryType` in `CategoryAppearance.kt`. → The handover's Scale/Slider/Count map onto existing types; **Yes/No and Time are new**.
- **Icons** already exist (`iconName` → `CategoryIcon`, 20 icons).
- **Allow-multiple-per-day** already exists (`allowMultiple`, honoured in `saveLog`).
- **Timestamps / timeline** partially exist (`trackAgainstTime` + `loggedAt` + `TimedIncrementSection`). → The handover's generalised timeline extends this to all input types.
- **Groups / roles**: **NONE exist.** No group entity, no role field, no parent-child. Closest existing mechanisms: `modeKey`, `systemKey`, `showInLogPeriod`, `custom_alarm_categories` join table. A group/role model is entirely additive.
