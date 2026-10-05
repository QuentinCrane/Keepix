package com.futureape.kanleme.ui.components

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MorphMotionTest {
    private val source = Rect(24f, 80f, 160f, 200f)
    private val target = Rect(0f, 0f, 1080f, 1920f)

    @Test fun boundsKeepBothEndpointsAndClampOvershoot() {
        assertEquals(source, MorphMotion.bounds(source, target, -0.1f))
        assertEquals(target, MorphMotion.bounds(source, target, 1.1f))
        val half = MorphMotion.bounds(source, target, 0.5f)
        assertEquals(12f, half.left, 0f)
        assertEquals(1060f, half.bottom, 0f)
    }

    @Test fun reversingTheClockRetracesExactlyTheSameGeometry() {
        val outward = MorphMotion.bounds(source, target, 0.6f)
        val inward = MorphMotion.bounds(target, source, 0.4f)
        assertEquals(outward.left, inward.left, 0.0001f)
        assertEquals(outward.width, inward.width, 0.0001f)
        assertEquals(outward.height, inward.height, 0.0001f)
    }

    @Test fun contentWaitsForSurfaceAndPartialReturnsTakeLessTime() {
        assertEquals(0f, MorphMotion.contentAlpha(0.30f), 0f)
        assertEquals(1f, MorphMotion.contentAlpha(1f), 0f)
        assertTrue(MorphMotion.duration(0.2f, 0f) < MorphMotion.DurationMillis)
        assertEquals(0, MorphMotion.duration(0f, 0f))
    }

    @Test fun nestedReturnUsesItsOwnSourceAndDisposedEntriesAreReleased() {
        val registry = MorphRegistry()
        registry.navigateTo("home")
        val first = MorphOrigin("home/card", "home", source)
        registry.pending = first
        registry.navigateTo("detail")
        registry.pending = MorphOrigin("detail/photo", "detail", source)
        registry.navigateTo("viewer")
        registry.navigateTo("detail")
        assertEquals("viewer", registry.movingEntryId)
        registry.releaseDisposed("viewer")
        assertNull(registry.originFor("viewer"))
        registry.navigateTo("home")
        assertEquals("detail", registry.movingEntryId)
        assertEquals(first, registry.originFor("detail"))
    }

    @Test fun reopeningDuringCollapseRetainsTheSameOrigin() {
        val registry = MorphRegistry()
        registry.navigateTo("home")
        val first = MorphOrigin("home/card", "home", source)
        registry.pending = first
        registry.navigateTo("detail")
        registry.navigateTo("home")
        registry.navigateTo("detail")
        assertEquals("detail", registry.movingEntryId)
        assertEquals(first, registry.originFor("detail"))
    }
}
