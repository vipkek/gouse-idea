package com.looshch.gouse.idea.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.OptionTag

data class GouseSettingsState(
    @OptionTag("gouse.path")
    var path: String = "",
    @OptionTag("gouse.autoUpdateOnStartup")
    var autoUpdateOnStartup: Boolean = true,
    @OptionTag("gouse.executablePath")
    var legacyExecutablePath: String = "",
)

@State(
    name = "GouseSettings",
    storages = [Storage("gouse.xml")],
)
class GouseSettingsService : PersistentStateComponent<GouseSettingsState> {
    private var state = GouseSettingsState()

    override fun getState(): GouseSettingsState = state

    override fun loadState(state: GouseSettingsState) {
        this.state = state
        if (this.state.path.isBlank() && this.state.legacyExecutablePath.isNotBlank()) {
            this.state.path = this.state.legacyExecutablePath.trim()
        }
    }

    fun getConfiguredExecutablePath(): String {
        val path = state.path.trim()
        if (path.isNotEmpty()) {
            return path
        }

        return state.legacyExecutablePath.trim()
    }

    fun setConfiguredExecutablePath(path: String) {
        val normalizedPath = path.trim()
        state.path = normalizedPath
        state.legacyExecutablePath = normalizedPath
    }

    fun getAutoUpdateOnStartup(): Boolean = state.autoUpdateOnStartup

    fun setAutoUpdateOnStartup(enabled: Boolean) {
        state.autoUpdateOnStartup = enabled
    }

    companion object {
        fun getInstance(): GouseSettingsService =
            ApplicationManager.getApplication().getService(GouseSettingsService::class.java)
    }
}
