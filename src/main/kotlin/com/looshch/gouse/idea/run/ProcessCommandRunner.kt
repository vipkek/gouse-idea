package com.looshch.gouse.idea.run

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler

class ProcessCommandRunner : CommandRunner {
    override fun run(command: List<String>): CommandResult {
        val executablePath = command.firstOrNull()
            ?: throw CommandExecutionException(
                message = "Missing executable path.",
                kind = CommandExecutionException.FailureKind.EXECUTION_ERROR,
            )

        val arguments = if (command.size > 1) command.drop(1) else emptyList()
        val commandLine = GeneralCommandLine(executablePath).withParameters(arguments)

        return try {
            val processOutput = CapturingProcessHandler(commandLine).runProcess()
            CommandResult(
                exitCode = processOutput.exitCode,
                stdout = processOutput.stdout,
                stderr = processOutput.stderr,
            )
        } catch (error: Exception) {
            val missingCommand = isMissingCommandError(error)
            val message = bestMessage(
                error = error,
                fallback = if (missingCommand) {
                    "The executable could not be started."
                } else {
                    "The command failed to start."
                },
            )
            throw CommandExecutionException(
                message = message,
                kind = if (missingCommand) {
                    CommandExecutionException.FailureKind.MISSING_COMMAND
                } else {
                    CommandExecutionException.FailureKind.EXECUTION_ERROR
                },
                cause = error,
            )
        }
    }

    private fun isMissingCommandError(error: Throwable): Boolean {
        if (exceptionChain(error).any { it is java.io.IOException }) {
            return true
        }

        val message = exceptionChain(error)
            .mapNotNull { it.message }
            .joinToString(" ")
            .lowercase()

        return message.contains("no such file or directory") ||
            message.contains("cannot run program") && message.contains("error=2") ||
            message.contains("createprocess error=2")
    }

    private fun bestMessage(error: Throwable, fallback: String): String {
        return exceptionChain(error)
            .mapNotNull { it.message?.takeIf { message -> message.isNotBlank() } }
            .firstOrNull()
            ?: fallback
    }

    private fun exceptionChain(error: Throwable): Sequence<Throwable> =
        generateSequence(error) { it.cause }
}
