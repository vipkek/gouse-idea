package com.looshch.gouse.idea.run

sealed interface InstallResult {
    data class Success(val executablePath: String?) : InstallResult
    data object GoMissing : InstallResult
    data class Failed(val message: String) : InstallResult
}

class GouseInstaller(
    private val commandRunner: CommandRunner,
    private val binaryResolver: GouseBinaryResolver,
) {
    fun install(): InstallResult {
        val installOutput = try {
            commandRunner.run(listOf("go", "install", "github.com/looshch/gouse/v2@latest"))
        } catch (error: CommandExecutionException) {
            return if (error.kind == CommandExecutionException.FailureKind.MISSING_COMMAND) {
                InstallResult.GoMissing
            } else {
                InstallResult.Failed(error.message)
            }
        }

        if (installOutput.exitCode != 0) {
            return InstallResult.Failed(errorMessageFor(installOutput))
        }

        val goEnvOutput = try {
            commandRunner.run(listOf("go", "env", "GOBIN", "GOPATH"))
        } catch (_: CommandExecutionException) {
            return InstallResult.Success(null)
        }

        if (goEnvOutput.exitCode != 0) {
            return InstallResult.Success(null)
        }

        val installedPath = binaryResolver.parseInstalledExecutablePath(goEnvOutput.stdout)
        binaryResolver.rememberInstalledExecutablePath(installedPath)
        return InstallResult.Success(installedPath)
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
}
