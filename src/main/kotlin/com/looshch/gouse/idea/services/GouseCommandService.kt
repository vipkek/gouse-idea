package com.looshch.gouse.idea.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.looshch.gouse.idea.GouseBundle
import com.looshch.gouse.idea.run.CommandExecutionException
import com.looshch.gouse.idea.run.CommandResult
import com.looshch.gouse.idea.run.CommandRunner
import com.looshch.gouse.idea.run.GouseBinaryResolver
import com.looshch.gouse.idea.run.GouseInstaller
import com.looshch.gouse.idea.run.GouseTarget
import com.looshch.gouse.idea.run.InstallResult
import com.looshch.gouse.idea.run.ProcessCommandRunner
import com.looshch.gouse.idea.run.RunResult

class GouseCommandService(
    private val settingsService: GouseSettingsService = GouseSettingsService.getInstance(),
    private val commandRunner: CommandRunner = ProcessCommandRunner(),
    private val binaryResolver: GouseBinaryResolver = GouseBinaryResolver(settingsService),
    private val installer: GouseInstaller = GouseInstaller(commandRunner, binaryResolver),
) {
    fun autoUpdateOnStartup(): String? {
        if (!settingsService.getAutoUpdateOnStartup()) {
            return null
        }

        if (settingsService.getConfiguredExecutablePath().isNotEmpty()) {
            return null
        }

        try {
            commandRunner.run(listOf("gouse", "-v"))
        } catch (error: CommandExecutionException) {
            if (error.kind == CommandExecutionException.FailureKind.MISSING_COMMAND) {
                return null
            }
        }

        return when (val installResult = installer.install()) {
            is InstallResult.Success -> null
            is InstallResult.GoMissing -> GouseBundle.message(
                "error.auto.update.failed",
                GouseBundle.message("error.go.missing.short"),
            )
            is InstallResult.Failed -> GouseBundle.message("error.auto.update.failed", installResult.message)
        }
    }

    fun runOnFile(_project: Project, virtualFile: VirtualFile): RunResult {
        val invalidTarget = validateAndSave(virtualFile)
        if (invalidTarget != null) {
            return invalidTarget
        }

        val executablePath = binaryResolver.resolveExecutablePath()
        val runResult = runGouse(executablePath, virtualFile.path)
        if (runResult != RunResult.Success) {
            return runResult
        }

        return refresh(virtualFile)
    }

    fun installAndRun(_project: Project, virtualFile: VirtualFile): RunResult {
        val invalidTarget = validateAndSave(virtualFile)
        if (invalidTarget != null) {
            return invalidTarget
        }

        return when (val installResult = installer.install()) {
            is InstallResult.GoMissing -> RunResult.GoMissing
            is InstallResult.Failed -> RunResult.ExecutionFailed(
                GouseBundle.message("error.install.failed", installResult.message),
            )
            is InstallResult.Success -> {
                val executablePath = installResult.executablePath ?: binaryResolver.resolveExecutablePath()
                val runResult = runGouse(executablePath, virtualFile.path)
                when (runResult) {
                    is RunResult.Success -> refresh(virtualFile)
                    is RunResult.ExecutionFailed -> RunResult.ExecutionFailed(
                        GouseBundle.message("error.gouse.failed.after.install", runResult.message),
                    )
                    else -> runResult
                }
            }
        }
    }

    private fun validateAndSave(virtualFile: VirtualFile): RunResult? {
        val targetErrorKey = GouseTarget.validationError(virtualFile)
        if (targetErrorKey != null) {
            return RunResult.InvalidTarget(GouseBundle.message(targetErrorKey))
        }

        if (!save(virtualFile)) {
            return RunResult.InvalidTarget(GouseBundle.message("error.save.before.run"))
        }

        return null
    }

    private fun save(virtualFile: VirtualFile): Boolean {
        if (!virtualFile.isWritable) {
            return false
        }

        val application = ApplicationManager.getApplication()
        var saved = false

        application.invokeAndWait {
            val manager = FileDocumentManager.getInstance()
            val document = manager.getDocument(virtualFile)
            if (document != null) {
                manager.saveDocument(document)
                saved = !manager.isDocumentUnsaved(document)
            } else {
                manager.saveAllDocuments()
                val cachedDocument = manager.getCachedDocument(virtualFile)
                saved = cachedDocument == null || !manager.isDocumentUnsaved(cachedDocument)
            }
        }

        return saved
    }

    internal fun runGouse(executablePath: String, targetPath: String): RunResult {
        val configuredPath = settingsService.getConfiguredExecutablePath()
        val commandResult = try {
            commandRunner.run(listOf(executablePath, "-w", targetPath))
        } catch (error: CommandExecutionException) {
            return if (error.kind == CommandExecutionException.FailureKind.MISSING_COMMAND) {
                if (configuredPath.isNotEmpty()) {
                    RunResult.ConfiguredPathMissing(configuredPath)
                } else {
                    runWithDiscoveredExecutable(
                        failedExecutablePath = executablePath,
                        targetPath = targetPath,
                    )
                }
            } else {
                RunResult.ExecutionFailed(error.message)
            }
        }

        if (commandResult.exitCode != 0) {
            return RunResult.ExecutionFailed(errorMessageFor(commandResult))
        }

        return RunResult.Success
    }

    private fun runWithDiscoveredExecutable(
        failedExecutablePath: String,
        targetPath: String,
    ): RunResult {
        val discoveredPath = discoverInstalledExecutablePath()
            ?.takeIf { it != failedExecutablePath }
            ?: return RunResult.GouseMissing

        val commandResult = try {
            commandRunner.run(listOf(discoveredPath, "-w", targetPath))
        } catch (error: CommandExecutionException) {
            return if (error.kind == CommandExecutionException.FailureKind.MISSING_COMMAND) {
                RunResult.GouseMissing
            } else {
                RunResult.ExecutionFailed(error.message)
            }
        }

        if (commandResult.exitCode != 0) {
            return RunResult.ExecutionFailed(errorMessageFor(commandResult))
        }

        binaryResolver.rememberInstalledExecutablePath(discoveredPath)
        return RunResult.Success
    }

    private fun discoverInstalledExecutablePath(): String? {
        val goEnvOutput = try {
            commandRunner.run(listOf("go", "env", "GOBIN", "GOPATH"))
        } catch (_: CommandExecutionException) {
            return null
        }

        if (goEnvOutput.exitCode != 0) {
            return null
        }

        return binaryResolver.parseInstalledExecutablePath(goEnvOutput.stdout)
    }

    private fun refresh(virtualFile: VirtualFile): RunResult {
        return try {
            ApplicationManager.getApplication().invokeAndWait {
                virtualFile.refresh(false, false)
                FileDocumentManager.getInstance().reloadFiles(virtualFile)
            }
            RunResult.Success
        } catch (error: Exception) {
            RunResult.RefreshFailed(error.message ?: "Unknown error")
        }
    }

    private fun errorMessageFor(result: CommandResult): String {
        val stderr = result.stderr.trim()
        if (stderr.isNotEmpty()) {
            return stderr
        }

        val stdout = result.stdout.trim()
        if (stdout.isNotEmpty()) {
            return stdout
        }

        return "Exit code ${result.exitCode}"
    }

    companion object {
        fun getInstance(): GouseCommandService =
            ApplicationManager.getApplication().getService(GouseCommandService::class.java)
    }
}
