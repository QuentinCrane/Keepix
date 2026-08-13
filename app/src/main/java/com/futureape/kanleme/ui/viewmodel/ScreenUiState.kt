package com.futureape.kanleme.ui.viewmodel

import com.futureape.kanleme.data.local.SimilarGroupEntity
import com.futureape.kanleme.data.repository.DashboardStats
import com.futureape.kanleme.data.repository.PhotoTypeStats

/** Stable, screen-scoped state contracts for screens that read more than one persistent Flow. */
data class MeScreenUiState(
    val dashboard: DashboardStats = DashboardStats(),
    val photoTypeStats: PhotoTypeStats = PhotoTypeStats(),
)

data class SimilarPhotosUiState(
    val groups: List<SimilarGroupEntity> = emptyList(),
    val detection: SimilarDetectionUiState = SimilarDetectionUiState(),
)
