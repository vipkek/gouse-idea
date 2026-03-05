package com.looshch.gouse.idea.run

import com.intellij.openapi.util.SystemInfoRt
import com.looshch.gouse.idea.services.GouseSettingsService
import java.nio.file.Paths

class GouseBinaryResolver(
    private val settingsService: GouseSettingsService,
) {
    private var installedExecutablePath: String? = null

    fun getConfiguredExecutablePath(): String = settingsService.getConfiguredExecutablePath()

    fun resolveExecutablePath(): String {
        val configured = getConfiguredExecutablePath()
        if (configured.isNotEmpty()) {
            return configured
        }

        return installedExecutablePath ?: defaultExecutableName()
    }

    fun rememberInstalledExecutablePath(path: String?) {
        installedExecutablePath = path?.takeIf { it.isNotBlank() }
    }

    fun parseInstalledExecutablePath(goEnvOutput: String): String? {
        val lines = goEnvOutput.split(Regex("\\r?\\n"))
        val goBin = lines.getOrNull(0)?.trim().orEmpty()
        if (goBin.isNotEmpty()) {
            return Paths.get(goBin, binaryName("gouse")).toString()
        }

        val goPath = lines.getOrNull(1)?.trim().orEmpty()
        if (goPath.isEmpty()) {
            return null
        }

        val firstGoPath = goPath.split(System.getProperty("path.separator")).firstOrNull { it.isNotBlank() }
            ?: return null

        return Paths.get(firstGoPath, "bin", binaryName("gouse")).toString()
    }

    fun binaryName(baseName: String): String =
        if (SystemInfoRt.isWindows) "$baseName.exe" else baseName

    private fun defaultExecutableName(): String = binaryName("gouse")
}
