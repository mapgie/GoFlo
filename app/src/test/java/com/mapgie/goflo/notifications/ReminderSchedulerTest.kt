package com.mapgie.goflo.notifications

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSchedulerTest {

    private val zone = ZoneId.of("America/New_York")

    private fun triggerAsLocal(hour: Int, minute: Int, now: LocalDateTime): ZonedDateTime {
        val millis = ReminderScheduler.nextTriggerMillis(hour, minute, now, zone)
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)
    }

    @Test
    fun `alarm later today fires today`() {
        val now = LocalDateTime.of(2026, 7, 10, 8, 0)
        val trigger = triggerAsLocal(21, 30, now)
        assertEquals(LocalDateTime.of(2026, 7, 10, 21, 30), trigger.toLocalDateTime())
    }

    @Test
    fun `alarm earlier today fires tomorrow`() {
        val now = LocalDateTime.of(2026, 7, 10, 8, 0)
        val trigger = triggerAsLocal(7, 0, now)
        assertEquals(LocalDateTime.of(2026, 7, 11, 7, 0), trigger.toLocalDateTime())
    }

    @Test
    fun `alarm at exactly the current minute fires tomorrow, not in the past`() {
        val now = LocalDateTime.of(2026, 7, 10, 8, 0)
        val trigger = triggerAsLocal(8, 0, now)
        assertEquals(LocalDateTime.of(2026, 7, 11, 8, 0), trigger.toLocalDateTime())
    }

    @Test
    fun `local alarm time is preserved across the spring DST transition`() {
        // US clocks spring forward on 2026-03-08. A 09:00 alarm scheduled the evening
        // before must fire at 09:00 local on the 8th (a 23-hour gap in absolute time),
        // which flat +24h millis arithmetic gets wrong by an hour.
        val now = LocalDateTime.of(2026, 3, 7, 22, 0)
        val trigger = triggerAsLocal(9, 0, now)
        assertEquals(LocalDateTime.of(2026, 3, 8, 9, 0), trigger.toLocalDateTime())
    }

    @Test
    fun `local alarm time is preserved across the autumn DST transition`() {
        // US clocks fall back on 2026-11-01: the day is 25 hours long.
        val now = LocalDateTime.of(2026, 10, 31, 22, 0)
        val trigger = triggerAsLocal(9, 0, now)
        assertEquals(LocalDateTime.of(2026, 11, 1, 9, 0), trigger.toLocalDateTime())
    }
}
