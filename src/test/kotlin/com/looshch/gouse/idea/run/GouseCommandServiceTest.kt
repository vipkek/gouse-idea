package com.looshch.gouse.idea.run

import com.looshch.gouse.idea.services.GouseCommandService
import com.looshch.gouse.idea.services.GouseSettingsService
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GouseCommandServiceTest {
    @Test
    fun `maps configured missing binary to configured path result`() {
        val settings = GouseSettingsService()
        settings.setConfiguredExecutablePath("/missing/gouse")
        val resolver = GouseBinaryResolver(settings)
        val runner = CommandRunner { _ ->
            throw CommandExecutionException(
                message = "missing",
                kind = CommandExecutionException.FailureKind.MISSING_COMMAND,
            )
        }

        val service = serviceWith(settings, runner, resolver)

        val result = service.runGouse(resolver.resolveExecutablePath(), "/tmp/example.go")

        assertEquals(RunResult.ConfiguredPathMissing("/missing/gouse"), result)
    }

    @Test
    fun `maps missing binary without configured path to gouse missing`() {
        val settings = GouseSettingsService()
        val resolver = GouseBinaryResolver(settings)
        val runner = CommandRunner { _ ->
            throw CommandExecutionException(
                message = "missing",
                kind = CommandExecutionException.FailureKind.MISSING_COMMAND,
            )
        }

        val service = serviceWith(settings, runner, resolver)

        val result = service.runGouse(resolver.resolveExecutablePath(), "/tmp/example.go")

        assertEquals(RunResult.GouseMissing, result)
    }

    @Test
    fun `returns execution failure for non-zero gouse exit`() {
        val settings = GouseSettingsService()
        val resolver = GouseBinaryResolver(settings)
        val runner = CommandRunner { _ ->
            CommandResult(exitCode = 1, stdout = "", stderr = "permission denied")
        }

        val service = serviceWith(settings, runner, resolver)

        val result = service.runGouse("gouse", "/tmp/example.go")

        assertEquals(RunResult.ExecutionFailed("permission denied"), result)
    }

    @Test
    fun `auto-update does nothing when disabled`() {
        val settings = GouseSettingsService().apply {
            setAutoUpdateOnStartup(false)
        }
        val commands = mutableListOf<List<String>>()
        val runner = CommandRunner { command ->
            commands += command
            CommandResult(0, "", "")
        }
        val resolver = GouseBinaryResolver(settings)
        val service = serviceWith(settings, runner, resolver)

        val warning = service.autoUpdateOnStartup()

        assertNull(warning)
        assertEquals(emptyList(), commands)
    }

    @Test
    fun `auto-update does nothing when gouse path is configured`() {
        val settings = GouseSettingsService().apply {
            setConfiguredExecutablePath("/custom/tools/gouse")
        }
        val commands = mutableListOf<List<String>>()
        val runner = CommandRunner { command ->
            commands += command
            CommandResult(0, "", "")
        }
        val resolver = GouseBinaryResolver(settings)
        val service = serviceWith(settings, runner, resolver)

        val warning = service.autoUpdateOnStartup()

        assertNull(warning)
        assertEquals(emptyList(), commands)
    }

    @Test
    fun `auto-update skips when gouse is missing`() {
        val settings = GouseSettingsService()
        val commands = mutableListOf<List<String>>()
        val runner = CommandRunner { command ->
            commands += command
            if (command == listOf("gouse", "-v")) {
                throw CommandExecutionException(
                    message = "gouse missing",
                    kind = CommandExecutionException.FailureKind.MISSING_COMMAND,
                )
            }
            error("Unexpected command: $command")
        }
        val resolver = GouseBinaryResolver(settings)
        val service = serviceWith(settings, runner, resolver)

        val warning = service.autoUpdateOnStartup()

        assertNull(warning)
        assertEquals(listOf(listOf("gouse", "-v")), commands)
    }

    @Test
    fun `auto-update installs and remembers installed path when gouse is present`() {
        val settings = GouseSettingsService()
        val commands = mutableListOf<List<String>>()
        val resolver = GouseBinaryResolver(settings)
        val runner = CommandRunner { command ->
            commands += command
            when (command) {
                listOf("gouse", "-v") ->
                    CommandResult(0, "gouse v0.7.0\n", "")
                listOf("go", "install", "github.com/looshch/gouse/v2@latest") ->
                    CommandResult(0, "", "")
                listOf("go", "env", "GOBIN", "GOPATH") ->
                    CommandResult(0, "/custom/bin\n/custom/go\n", "")
                else -> error("Unexpected command: $command")
            }
        }
        val service = serviceWith(settings, runner, resolver)

        val warning = service.autoUpdateOnStartup()

        assertNull(warning)
        assertEquals(
            listOf(
                listOf("gouse", "-v"),
                listOf("go", "install", "github.com/looshch/gouse/v2@latest"),
                listOf("go", "env", "GOBIN", "GOPATH"),
            ),
            commands,
        )
        assertEquals("/custom/bin/${resolver.binaryName("gouse")}", resolver.resolveExecutablePath())
    }

    @Test
    fun `auto-update returns warning when install fails`() {
        val settings = GouseSettingsService()
        val runner = CommandRunner { command ->
            when (command) {
                listOf("gouse", "-v") ->
                    CommandResult(0, "gouse v0.7.0\n", "")
                listOf("go", "install", "github.com/looshch/gouse/v2@latest") ->
                    CommandResult(1, "", "network timeout")
                else -> error("Unexpected command: $command")
            }
        }
        val resolver = GouseBinaryResolver(settings)
        val service = serviceWith(settings, runner, resolver)

        val warning = service.autoUpdateOnStartup()

        requireNotNull(warning)
        assertContains(warning, "gouse auto-update failed:")
        assertContains(warning, "network timeout")
    }

    @Test
    fun `auto-update returns warning when go is missing`() {
        val settings = GouseSettingsService()
        val runner = CommandRunner { command ->
            when (command) {
                listOf("gouse", "-v") ->
                    CommandResult(0, "gouse v0.7.0\n", "")
                listOf("go", "install", "github.com/looshch/gouse/v2@latest") ->
                    throw CommandExecutionException(
                        message = "go missing",
                        kind = CommandExecutionException.FailureKind.MISSING_COMMAND,
                    )
                else -> error("Unexpected command: $command")
            }
        }
        val resolver = GouseBinaryResolver(settings)
        val service = serviceWith(settings, runner, resolver)

        val warning = service.autoUpdateOnStartup()

        assertEquals(
            "gouse auto-update failed: Go is not installed or is not available on PATH.",
            warning,
        )
    }

    private fun serviceWith(
        settings: GouseSettingsService,
        runner: CommandRunner,
        resolver: GouseBinaryResolver,
    ): GouseCommandService =
        GouseCommandService(
            settingsService = settings,
            commandRunner = runner,
            binaryResolver = resolver,
            installer = GouseInstaller(runner, resolver),
        )
}
