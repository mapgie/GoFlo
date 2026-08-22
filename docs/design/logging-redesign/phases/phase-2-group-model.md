# Phase 2 — Group data model

**Goal:** groups exist in the data layer with colour inheritance. Additive migration 23→24. No UI redesign yet (management UI is Phase 6).

**Prerequisites:** Phase 1 (roles resolve, incl. the new tokens).
**Read first:** `subsystem-maps/02-category-data-model.md` (entities, migration chain, repository API); `PLAN.md` §5 (inheritance rule) and §8 (open decision #1).

---

## Why

A group owns a colour role and a default input type; a category optionally belongs to one. This is the only genuinely new table in the redesign. Everything additive so existing data survives untouched.

## The inheritance rule (important deviation)

The handover says existing categories should default to a neutral `surfaceVariant`. **We do not do that** — it would visually wipe the colours every existing category already has (`colorToken` is populated today). Instead:

- A category resolves colour from its **own `colorToken`**.
- Introduce a sentinel token `"inherit"`. If `colorToken == "inherit"` **and** `groupId != null`, resolve to the **group's `colorRole`**.
- If `colorToken == "inherit"` **and** `groupId == null`, render neutral (`surfaceVariant`).
- Existing rows keep their real `colorToken` (e.g. `"primary"`), so they look identical after migration. Only categories the user explicitly sets to "inherit" follow the group.

This keeps §8 decision #1 open (you can later offer an optional "Organise" nudge) without a destructive default. Confirm with the owner before shipping if unsure.

## Files to touch

| File | Change |
|---|---|
| `data/database/entities/Group.kt` | **New** entity. |
| `data/database/dao/GroupDao.kt` | **New** DAO. |
| `data/database/entities/TrackingCategory.kt` | Add `groupId: Long? = null`. |
| `data/database/GoFloDatabase.kt` | Add `Group` to `@Database`, bump `version = 24`, add `MIGRATION_23_24`, expose `groupDao()`. |
| `data/repository/TrackingRepository.kt` (or a new `GroupRepository`) | Group CRUD + assign/unassign category. |
| `ui/util/CategoryAppearance.kt` | Add `"inherit"` handling to the resolvers (needs the group's role passed in — see below). |
| `GoFloApplication.kt` | Wire the new DAO/repository if a separate repository is used. |

## Step-by-step

### 1. Entity (`Group.kt`)
```kotlin
@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorRole: String = "primary",      // a CategoryColor key (never a hex; groups are in-theme)
    val defaultInputType: String = "default",// a CategoryType key
    val displayOrder: Int = 0,
)
```

### 2. `TrackingCategory` — add nullable column
```kotlin
val groupId: Long? = null,
```
Place it last to keep the constructor call-sites that use named args safe (they mostly do). Grep for positional constructor calls just in case.

### 3. Migration (`GoFloDatabase.kt`)
Follow the additive-column pattern used by e.g. MIGRATION_11_12:
```kotlin
val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS `groups` (
            `id` INTEGER NOT NULL PRIMARY KEY AUTOGENERATE_PLACEHOLDER,
            `name` TEXT NOT NULL,
            `colorRole` TEXT NOT NULL DEFAULT 'primary',
            `defaultInputType` TEXT NOT NULL DEFAULT 'default',
            `displayOrder` INTEGER NOT NULL DEFAULT 0)""")
        db.execSQL("ALTER TABLE tracking_categories ADD COLUMN groupId INTEGER")
    }
}
```
> Match Room's exact generated SQL: autoGenerate PK is `INTEGER PRIMARY KEY AUTOINCREMENT` only if you keep AUTOINCREMENT; Room by default uses `INTEGER PRIMARY KEY AUTOINCREMENT` when `autoGenerate = true`. **Confirm the generated schema** — the safe way is to let Room generate it once (temporarily set `exportSchema = true` locally) or copy the column definition style from an existing `CREATE TABLE` migration in this file. `groupId` is nullable so no default is needed.

Register it in `.addMigrations(...)` and bump `version = 24`. Add `Group::class` to the `entities = [...]` array and a `abstract fun groupDao(): GroupDao`.

### 4. DAO (`GroupDao.kt`)
```kotlin
@Dao interface GroupDao {
    @Query("SELECT * FROM `groups` ORDER BY displayOrder, name") fun getAllGroups(): Flow<List<Group>>
    @Query("SELECT * FROM `groups` ORDER BY displayOrder, name") suspend fun getAllGroupsOnce(): List<Group>
    @Query("SELECT * FROM `groups` WHERE id = :id") suspend fun getGroupById(id: Long): Group?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertGroup(group: Group): Long
    @Update suspend fun updateGroup(group: Group)
    @Delete suspend fun deleteGroup(group: Group)
}
```
> Decide delete behaviour: deleting a group should **not** delete its categories. Either set their `groupId = null` in the same transaction, or `ON DELETE SET NULL` via an FK. Simplest: a repository method that nulls members then deletes the group. Do **not** cascade-delete categories.

### 5. Repository methods
`getAllGroups()`, `addGroup(name, colorRole, defaultInputType)`, `renameGroup`, `updateGroupRole`, `reorderGroups`, `deleteGroup(unassignMembers=true)`, `assignCategoryToGroup(categoryId, groupId)`, `unassignCategory(categoryId)`.

### 6. Resolver `"inherit"` support (`CategoryAppearance.kt`)
`toCategoryColor()` is `@Composable` and keyed only on the token string; it does not know the group. Two options:
- **A (recommended):** resolve the effective token *before* calling `toCategoryColor()` — at the call site you usually have the category and can look up its group's role. Add a helper `TrackingCategory.effectiveColorToken(groups): String` that returns `colorToken` unless it is `"inherit"`, in which case the group's `colorRole` (or `"surfaceVariant-sentinel"` when no group).
- **B:** pass an optional group role into the resolver. Messier; prefer A.

Add a neutral branch so an inherit-with-no-group category renders `surfaceVariant`/`onSurfaceVariant`.

## Data changes
Migration 23→24 adds the `groups` table and a nullable `groupId` column. Existing rows: `groupId = null`, colours unchanged.

## Acceptance criteria
- [ ] **Migration test:** open a seeded v23 database, run `MIGRATION_23_24`, assert (a) `groups` exists, (b) `tracking_categories.groupId` exists and is null for all rows, (c) all pre-existing category/value/log data is intact. (See how other migrations are exercised in tests, if any; otherwise add one under `app/src/test` or `androidTest`.)
- [ ] A category with `colorToken = "inherit"` assigned to a group renders the group's role colour and re-themes.
- [ ] An inherit category with no group renders neutral.
- [ ] Existing categories look **identical** to before (no grey wipe).
- [ ] App builds in CI.

## Feature-preservation checklist
- [ ] All existing category reads (`getActiveCategories`, `getCategoryById`, …) still compile and return the same data plus the new nullable field.
- [ ] Stats/History/Home/DayLogSheet/export unaffected (they don't need `groupId` yet).
- [ ] Deleting a group never deletes categories or their history.
- [ ] `saveLog` / value CRUD untouched.

## Gotchas
- Room requires the migration's generated SQL to **exactly** match the entity schema or it throws `IllegalStateException` at first open after the bump. Copy the column-definition style from an existing migration in this file; verify types (`INTEGER`/`TEXT`) and `NOT NULL`/defaults.
- `groups` is a SQL keyword-adjacent name; always backtick it in raw SQL.
- Never `fallbackToDestructiveMigration` (CLAUDE.md).
- If any `TrackingCategory(...)` positional constructor exists, adding a field breaks it — grep and fix to named args.

## Changelog fragment
```json
{ "bump": "minor", "added": ["Groups: categories can now belong to a group that owns a shared colour role"] }
```
