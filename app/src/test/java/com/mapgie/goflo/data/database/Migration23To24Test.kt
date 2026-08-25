package com.mapgie.goflo.data.database

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager

/**
 * Exercises the real [GoFloDatabase.MIGRATION_23_24] SQL against a real SQLite
 * database (via sqlite-jdbc) seeded with the v23 schema and representative data.
 *
 * Room's MigrationTestHelper is not usable here: it requires instrumented tests
 * and exported schema JSON, and this project has neither (exportSchema = false,
 * JVM-only test source set). Instead the migration object's execSQL calls are
 * routed to JDBC through a reflection proxy — MIGRATION_23_24 only ever calls
 * execSQL(String), so no other SupportSQLiteDatabase member is needed.
 *
 * The expected `groups` schema asserted below must match what Room generates
 * for the Group entity (including the DEFAULT clauses declared via
 * @ColumnInfo(defaultValue = ...)); Room validates the migrated schema against
 * the entity on first open, so a drift here is a crash on device.
 */
class Migration23To24Test {

    private lateinit var connection: Connection

    /** Routes SupportSQLiteDatabase.execSQL to JDBC; anything else fails the test. */
    private val supportDb: SupportSQLiteDatabase by lazy {
        Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            when (method.name) {
                "execSQL" -> {
                    connection.createStatement().use { it.execute(args!![0] as String) }
                    null
                }
                else -> throw UnsupportedOperationException(
                    "MIGRATION_23_24 called ${method.name}, which this test does not fake"
                )
            }
        } as SupportSQLiteDatabase
    }

    @Before
    fun createV23Database() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        exec("PRAGMA foreign_keys = ON")

        // The v23 shape of the four tracking tables, as produced by the real
        // migration chain (MIGRATION_6_7 rebuild + subsequent ADD COLUMNs).
        exec(
            """CREATE TABLE tracking_categories
               (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `isSystem` INTEGER NOT NULL DEFAULT 0,
                `displayOrder` INTEGER NOT NULL DEFAULT 0,
                `iconName` TEXT NOT NULL DEFAULT 'category',
                `colorToken` TEXT NOT NULL DEFAULT 'secondary',
                `categoryType` TEXT NOT NULL DEFAULT 'default',
                `numericMin` REAL NOT NULL DEFAULT 0.0,
                `numericMax` REAL NOT NULL DEFAULT 10.0,
                `allowDecimals` INTEGER NOT NULL DEFAULT 0,
                `numericUnit` TEXT NOT NULL DEFAULT '',
                `isArchived` INTEGER NOT NULL DEFAULT 0,
                `allowMultiple` INTEGER NOT NULL DEFAULT 0,
                `showInLogPeriod` INTEGER NOT NULL DEFAULT 0,
                `scaleLabels` TEXT NOT NULL DEFAULT '',
                `systemKey` TEXT NOT NULL DEFAULT '',
                `trackAgainstTime` INTEGER NOT NULL DEFAULT 0,
                `modeKey` TEXT NOT NULL DEFAULT '')"""
        )
        exec(
            """CREATE TABLE tracking_values
               (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `label` TEXT NOT NULL,
                `displayOrder` INTEGER NOT NULL DEFAULT 0,
                `isSeeded` INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(`categoryId`) REFERENCES `tracking_categories`(`id`) ON DELETE CASCADE)"""
        )
        exec(
            """CREATE TABLE tracking_logs
               (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `date` TEXT NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `notes` TEXT NOT NULL DEFAULT '',
                `loggedAt` TEXT NOT NULL DEFAULT '',
                FOREIGN KEY(`categoryId`) REFERENCES `tracking_categories`(`id`) ON DELETE CASCADE)"""
        )
        exec(
            """CREATE TABLE tracking_log_values
               (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `logId` INTEGER NOT NULL,
                `valueLabel` TEXT NOT NULL,
                FOREIGN KEY(`logId`) REFERENCES `tracking_logs`(`id`) ON DELETE CASCADE)"""
        )

        // Representative v23 data: the two seeded system categories with their
        // real colour tokens, one custom category, catalog values, and logs.
        exec(
            "INSERT INTO tracking_categories (id, name, isSystem, systemKey, displayOrder, iconName, colorToken, showInLogPeriod) " +
                "VALUES (1, 'Flow', 1, 'flow', 0, 'water', 'primary', 1)"
        )
        exec(
            "INSERT INTO tracking_categories (id, name, isSystem, systemKey, displayOrder, iconName, colorToken, showInLogPeriod) " +
                "VALUES (2, 'Symptoms', 1, 'symptoms', 1, 'healing', 'tertiary', 1)"
        )
        exec(
            "INSERT INTO tracking_categories (id, name, displayOrder, iconName, colorToken, categoryType, allowMultiple, trackAgainstTime) " +
                "VALUES (3, 'Mood', 2, 'mood', 'quaternary', 'numeric_slider', 1, 1)"
        )
        listOf("Spotting", "Light", "Medium", "Heavy").forEachIndexed { i, label ->
            exec("INSERT INTO tracking_values (categoryId, label, displayOrder, isSeeded) VALUES (1, '$label', $i, 1)")
        }
        exec("INSERT INTO tracking_logs (id, date, categoryId, notes, loggedAt) VALUES (1, '2026-08-01', 1, 'a note', '')")
        exec("INSERT INTO tracking_logs (id, date, categoryId, notes, loggedAt) VALUES (2, '2026-08-01', 3, '', '09:30')")
        exec("INSERT INTO tracking_log_values (logId, valueLabel) VALUES (1, 'Medium')")
        exec("INSERT INTO tracking_log_values (logId, valueLabel) VALUES (2, '4')")
    }

    @After
    fun tearDown() {
        connection.close()
    }

    @Test
    fun `creates groups table with the exact schema Room expects`() {
        GoFloDatabase.MIGRATION_23_24.migrate(supportDb)

        // (name, type, notnull, dflt_value, pk) per column, in declaration order.
        val columns = tableInfo("groups")
        assertEquals(
            listOf(
                listOf("id", "INTEGER", 1, null, 1),
                listOf("name", "TEXT", 1, null, 0),
                listOf("colorRole", "TEXT", 1, "'primary'", 0),
                listOf("defaultInputType", "TEXT", 1, "'default'", 0),
                listOf("displayOrder", "INTEGER", 1, "0", 0),
            ),
            columns
        )

        // autoGenerate = true → Room expects AUTOINCREMENT on the PK.
        val createSql = queryString("SELECT sql FROM sqlite_master WHERE type='table' AND name='groups'")
        assertTrue("groups PK must be AUTOINCREMENT", createSql!!.contains("AUTOINCREMENT"))
    }

    @Test
    fun `adds nullable groupId column defaulting to null on every existing row`() {
        GoFloDatabase.MIGRATION_23_24.migrate(supportDb)

        val groupIdColumn = tableInfo("tracking_categories").firstOrNull { it[0] == "groupId" }
        assertEquals(listOf("groupId", "INTEGER", 0, null, 0), groupIdColumn)

        assertEquals(3, queryInt("SELECT COUNT(*) FROM tracking_categories"))
        assertEquals(0, queryInt("SELECT COUNT(*) FROM tracking_categories WHERE groupId IS NOT NULL"))
    }

    @Test
    fun `preserves all pre-existing category, value, and log data`() {
        GoFloDatabase.MIGRATION_23_24.migrate(supportDb)

        assertEquals(4, queryInt("SELECT COUNT(*) FROM tracking_values"))
        assertEquals(2, queryInt("SELECT COUNT(*) FROM tracking_logs"))
        assertEquals(2, queryInt("SELECT COUNT(*) FROM tracking_log_values"))

        // Colour tokens survive untouched — the no-grey-wipe guarantee.
        assertEquals("primary", queryString("SELECT colorToken FROM tracking_categories WHERE id = 1"))
        assertEquals("tertiary", queryString("SELECT colorToken FROM tracking_categories WHERE id = 2"))
        assertEquals("quaternary", queryString("SELECT colorToken FROM tracking_categories WHERE id = 3"))

        // Spot-check a full custom-category row and a log's linkage.
        connection.createStatement().use { st ->
            val rs = st.executeQuery(
                "SELECT name, categoryType, allowMultiple, trackAgainstTime, groupId FROM tracking_categories WHERE id = 3"
            )
            assertTrue(rs.next())
            assertEquals("Mood", rs.getString("name"))
            assertEquals("numeric_slider", rs.getString("categoryType"))
            assertEquals(1, rs.getInt("allowMultiple"))
            assertEquals(1, rs.getInt("trackAgainstTime"))
            rs.getLong("groupId")
            assertTrue(rs.wasNull())
        }
        assertEquals("Medium", queryString("SELECT valueLabel FROM tracking_log_values WHERE logId = 1"))
        assertEquals("09:30", queryString("SELECT loggedAt FROM tracking_logs WHERE id = 2"))
    }

    @Test
    fun `groups table accepts inserts and supports the unfile-on-delete flow`() {
        GoFloDatabase.MIGRATION_23_24.migrate(supportDb)

        exec("INSERT INTO `groups` (name, colorRole, defaultInputType, displayOrder) VALUES ('Sleep', 'quinary', 'numeric_slider', 0)")
        val groupId = queryInt("SELECT id FROM `groups` WHERE name = 'Sleep'")
        exec("UPDATE tracking_categories SET groupId = $groupId WHERE id = 3")
        assertEquals(groupId, queryInt("SELECT groupId FROM tracking_categories WHERE id = 3"))

        // Repository delete order: unfile members, then delete the group row.
        exec("UPDATE tracking_categories SET groupId = NULL WHERE groupId = $groupId")
        exec("DELETE FROM `groups` WHERE id = $groupId")
        assertNull(queryString("SELECT groupId FROM tracking_categories WHERE id = 3"))
        assertEquals(3, queryInt("SELECT COUNT(*) FROM tracking_categories"))
    }

    // ── JDBC helpers ──────────────────────────────────────────────────────────

    private fun exec(sql: String) {
        connection.createStatement().use { it.execute(sql) }
    }

    private fun queryInt(sql: String): Int =
        connection.createStatement().use { st ->
            st.executeQuery(sql).use { rs -> rs.next(); rs.getInt(1) }
        }

    private fun queryString(sql: String): String? =
        connection.createStatement().use { st ->
            st.executeQuery(sql).use { rs -> if (rs.next()) rs.getString(1) else null }
        }

    /** Returns PRAGMA table_info rows as (name, type, notnull, dflt_value, pk). */
    private fun tableInfo(table: String): List<List<Any?>> =
        connection.createStatement().use { st ->
            st.executeQuery("PRAGMA table_info(`$table`)").use { rs ->
                buildList {
                    while (rs.next()) {
                        add(
                            listOf(
                                rs.getString("name"),
                                rs.getString("type"),
                                rs.getInt("notnull"),
                                rs.getString("dflt_value"),
                                rs.getInt("pk"),
                            )
                        )
                    }
                }
            }
        }
}
