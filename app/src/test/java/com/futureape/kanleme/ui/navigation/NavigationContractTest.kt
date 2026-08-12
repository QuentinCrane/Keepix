package com.futureape.kanleme.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationContractTest {
    @Test
    fun shortcutsOpenCleaningRoutesDirectly() {
        assertEquals(Destinations.PHOTO, NavigationContract.routeForShortcut("photo"))
        assertEquals(Destinations.VIDEO, NavigationContract.routeForShortcut("video"))
    }

    @Test
    fun unknownShortcutDoesNotCreateAnIntermediateRoute() {
        assertNull(NavigationContract.routeForShortcut(null))
        assertNull(NavigationContract.routeForShortcut("photo_start"))
        assertNull(NavigationContract.routeForShortcut("video_start"))
    }

    @Test
    fun primaryNavigationOrderRemainsStable() {
        assertEquals(
            listOf(Destinations.HOME, Destinations.VIDEO, Destinations.ME),
            NavigationContract.primaryRoutes,
        )
    }
}
