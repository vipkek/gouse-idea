package com.looshch.gouse.idea.services

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GouseSettingsServiceTest {
    @Test
    fun `migrates legacy executable path when new path is empty`() {
        val service = GouseSettingsService()
        service.loadState(
            GouseSettingsState(
                path = "",
                legacyExecutablePath = "/legacy/gouse",
            ),
        )

        assertEquals("/legacy/gouse", service.getConfiguredExecutablePath())
    }

    @Test
    fun `new path takes precedence over legacy path`() {
        val service = GouseSettingsService()
        service.loadState(
            GouseSettingsState(
                path = "/new/gouse",
                legacyExecutablePath = "/legacy/gouse",
            ),
        )

        assertEquals("/new/gouse", service.getConfiguredExecutablePath())
    }

    @Test
    fun `auto update defaults to enabled`() {
        val service = GouseSettingsService()

        assertTrue(service.getAutoUpdateOnStartup())
    }

    @Test
    fun `setting configured path keeps legacy field in sync`() {
        val service = GouseSettingsService()

        service.setConfiguredExecutablePath("/tmp/gouse")

        assertEquals("/tmp/gouse", service.getState().path)
        assertEquals("/tmp/gouse", service.getState().legacyExecutablePath)
    }
}
