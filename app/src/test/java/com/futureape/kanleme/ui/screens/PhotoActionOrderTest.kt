package com.futureape.kanleme.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoActionOrderTest {
    @Test
    fun defaultOrderKeepsShareBeforeUndo() {
        assertEquals(
            listOf(PhotoBottomAction.SHARE, PhotoBottomAction.INFO, PhotoBottomAction.UNDO),
            photoBottomActionOrder(swapShareAndUndo = false),
        )
    }

    @Test
    fun swapOnlyChangesVisualOrder() {
        assertEquals(
            listOf(PhotoBottomAction.UNDO, PhotoBottomAction.INFO, PhotoBottomAction.SHARE),
            photoBottomActionOrder(swapShareAndUndo = true),
        )
    }
}
