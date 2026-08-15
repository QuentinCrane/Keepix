package com.futureape.kanleme.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun criticalJourneys() = baselineProfileRule.collect(
        packageName = APP_PACKAGE,
        profileBlock = {
            startActivityAndWait()
            prepareFirstRunState(device)
            waitForIdle(device)

            // Keep the launcher path in the profile, then exercise both cleanup entry points.
            navigateBottomTab(device, "视频")
            device.swipe(
                device.displayWidth / 2,
                (device.displayHeight * 0.78f).toInt(),
                device.displayWidth / 2,
                (device.displayHeight * 0.28f).toInt(),
                18,
            )
            launchCleanupShortcut("photo")
            device.swipe(
                device.displayWidth / 2,
                (device.displayHeight * 0.32f).toInt(),
                device.displayWidth / 2,
                (device.displayHeight * 0.78f).toInt(),
                18,
            )
            launchCleanupShortcut("video")
            device.swipe(
                device.displayWidth / 2,
                (device.displayHeight * 0.78f).toInt(),
                device.displayWidth / 2,
                (device.displayHeight * 0.28f).toInt(),
                18,
            )

            // The settings button is intentionally reached through the visible Me route.
            navigateBottomTab(device, "我的", "Me")
            clickTextIfPresent(device, "回收站", "Trash")
            waitForIdle(device)
            device.pressBack()
            device.click(device.displayWidth - 72, 96)
            waitForIdle(device)
            device.pressBack()
            killProcess()
        },
    )

    private fun prepareFirstRunState(device: UiDevice) {
        repeat(4) {
            if (clickTextIfPresent(device, "下一步", "Next")) {
                waitForIdle(device)
            }
        }
        clickTextIfPresent(device, "开始使用", "Get started")
        clickTextIfPresent(device, "授权相册访问", "Allow photo access", "授权")
        clickTextIfPresent(device, "允许全部", "Allow all")
        clickTextIfPresent(device, "允许", "Allow")
        waitForIdle(device)
    }

    private fun navigateBottomTab(device: UiDevice, vararg labels: String) {
        val node = labels
            .asSequence()
            .mapNotNull { label -> device.findObject(By.desc(label)) ?: device.findObject(By.text(label)) }
            .firstOrNull()
        node?.click()
        waitForIdle(device)
    }

    private fun MacrobenchmarkScope.launchCleanupShortcut(target: String) {
        // Macrobenchmark must own the kill so ART flushes the profile between journeys.
        killProcess()
        device.executeShellCommand(
            "am start -n $APP_PACKAGE/.MainActivity " +
                "-a com.futureape.kanleme.ACTION_CLEAN " +
                "--es shortcut_target $target",
        )
        waitForIdle(device)
    }

    private fun clickTextIfPresent(device: UiDevice, vararg texts: String): Boolean {
        val node = texts
            .asSequence()
            .mapNotNull { text -> device.findObject(By.text(text)) }
            .firstOrNull()
            ?: return false
        node.click()
        return true
    }

    private fun waitForIdle(device: UiDevice) {
        device.waitForIdle()
    }

    private companion object {
        const val APP_PACKAGE = "com.futureape.kanleme"
    }
}
