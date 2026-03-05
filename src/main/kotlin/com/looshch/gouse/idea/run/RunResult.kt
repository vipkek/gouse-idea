package com.looshch.gouse.idea.run

sealed interface RunResult {
    data object Success : RunResult
    data class InvalidTarget(val reason: String) : RunResult
    data class ConfiguredPathMissing(val path: String) : RunResult
    data object GouseMissing : RunResult
    data object GoMissing : RunResult
    data class ExecutionFailed(val message: String) : RunResult
    data class RefreshFailed(val message: String) : RunResult
}
