package com.futureape.kanleme.ui.screens

internal enum class PhotoBottomAction {
    SHARE,
    INFO,
    UNDO,
}

internal fun photoBottomActionOrder(swapShareAndUndo: Boolean): List<PhotoBottomAction> =
    if (swapShareAndUndo) {
        listOf(PhotoBottomAction.UNDO, PhotoBottomAction.INFO, PhotoBottomAction.SHARE)
    } else {
        listOf(PhotoBottomAction.SHARE, PhotoBottomAction.INFO, PhotoBottomAction.UNDO)
    }
