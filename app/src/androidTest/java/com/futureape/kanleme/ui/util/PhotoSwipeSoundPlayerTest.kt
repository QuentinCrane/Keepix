package com.futureape.kanleme.ui.util

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.futureape.kanleme.data.repository.SwipeAction
import com.futureape.kanleme.data.settings.SwipeSoundStyle
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoSwipeSoundPlayerTest {
    @Test
    fun eachBundledStyleAcceptsAllCommittedActionsAndReleases() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        SwipeSoundStyle.entries.forEach { style ->
            val player = PhotoSwipeSoundPlayer(context, enabled = true, style = style)
            SwipeAction.entries.forEach(player::play)
            player.release()
        }
    }

    @Test
    fun disabledPlayerIgnoresAllCommittedActions() {
        val player = PhotoSwipeSoundPlayer(
            ApplicationProvider.getApplicationContext(),
            enabled = false,
            style = SwipeSoundStyle.SOFT_BREEZE,
        )

        SwipeAction.entries.forEach(player::play)
        player.release()
    }
}
