package com.futureape.kanleme.ui.util

import java.time.Instant
import java.time.ZoneId

/** Bounded, context-free metadata cache. No formatter is constructed for each visible photo. */
internal object MediaDayKeyCache {
    private val dates = object : LinkedHashMap<Pair<Long, String>, String>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<Long, String>, String>) = size > 1024
    }

    @Synchronized
    fun dayKey(timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        dates.getOrPut(timestamp to zone.id) { Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate().toString() }
}
