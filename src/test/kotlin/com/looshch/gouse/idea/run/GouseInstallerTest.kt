package com.looshch.gouse.idea.run

import com.looshch.gouse.idea.services.GouseSettingsService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class GouseInstallerTest {
    @Test
    fun `installs and remembers resolved executable path`() {
        val settings = GouseSettingsService()
        val resolver = GouseBinaryResolver(settings)
        val commands = mutableListOf<List<String>>()
        val runner = CommandRunner { command ->
            commands += command
            when {
                command.take(2) == listOf("go", "install") ->
                    CommandResult(0, "", "")
                command.take(3) == listOf("go", "env", "GOBIN") ->
                    CommandResult(0, "/custom/bin\n/custom/go\n", "")
                else -> error("Unexpected command: $command")
            }
        }

        val installer = GouseInstaller(runner, resolver)
        val result = installer.install()

        val success = assertIs<InstallResult.Success>(result)
        assertEquals("/custom/bin/${resolver.binaryName("gouse")}", success.executablePath)
        assertEquals(success.executablePath, resolver.resolveExecutablePath())
        assertEquals(
            listOf(
                listOf("go", "install", "github.com/looshch/gouse/v2@latest"),
                listOf("go", "env", "GOBIN", "GOPATH"),
            ),
            commands,
        )
    }

    @Test
    fun `returns go missing when install command cannot be started`() {
        val settings = GouseSettingsService()
        val resolver = GouseBinaryResolver(settings)
        val runner = CommandRunner {
            throw CommandExecutionException(
                message = "go missing",
                kind = CommandExecutionException.FailureKind.MISSING_COMMAND,
            )
        }

        val installer = GouseInstaller(runner, resolver)

        assertIs<InstallResult.GoMissing>(installer.install())
    }

    @Test
    fun `returns failure for non-zero install exit`() {
        val settings = GouseSettingsService()
        val resolver = GouseBinaryResolver(settings)
        val runner = CommandRunner { _ ->
            CommandResult(1, "", "permission denied")
        }

        val installer = GouseInstaller(runner, resolver)
        val result = installer.install()

        val failure = assertIs<InstallResult.Failed>(result)
        assertEquals("permission denied", failure.message)
    }

    @Test
    fun `succeeds without resolved path when go env is unavailable`() {
        val settings = GouseSettingsService()
        val resolver = GouseBinaryResolver(settings)
        var callCount = 0
        val runner = CommandRunner { _ ->
            callCount += 1
            if (callCount == 1) {
                CommandResult(0, "", "")
            } else {
                throw CommandExecutionException(
                    message = "env missing",
                    kind = CommandExecutionException.FailureKind.EXECUTION_ERROR,
                )
            }
        }

        val installer = GouseInstaller(runner, resolver)
        val result = installer.install()

        val success = assertIs<InstallResult.Success>(result)
        assertNull(success.executablePath)
    }
}
