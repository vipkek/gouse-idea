package com.looshch.gouse.idea.run

data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

class CommandExecutionException(
    override val message: String,
    val kind: FailureKind,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    enum class FailureKind {
        MISSING_COMMAND,
        EXECUTION_ERROR,
    }
}

fun interface CommandRunner {
    @Throws(CommandExecutionException::class)
    fun run(command: List<String>): CommandResult
}
