package com.looshch.gouse.idea.intentions

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.lang.annotation.HighlightSeverity
import java.util.Locale

internal data class GouseDiagnosticSpan(
    val startOffset: Int,
    val endOffset: Int,
    val description: String?,
)

internal object GouseDiagnosticMatcher {
    private val UNUSED_DIAGNOSTIC_FRAGMENTS = listOf(
        "declared and not used",
        "unused variable",
    )
    private const val GOUSE_TODO_MARKER = "/* TODO: gouse */"

    fun matchingDiagnostics(project: Project, editor: Editor): List<GouseDiagnosticSpan> {
        val document = editor.document
        val diagnostics = mutableListOf<GouseDiagnosticSpan>()

        DaemonCodeAnalyzerEx.processHighlights(
            document,
            project,
            HighlightSeverity.INFORMATION,
            0,
            document.textLength,
        ) { highlight ->
            diagnostics += GouseDiagnosticSpan(
                startOffset = highlight.startOffset,
                endOffset = highlight.endOffset,
                description = highlight.description,
            )
            true
        }

        return diagnostics
            .let(::filterUnusedDiagnostics)
    }

    fun filterUnusedDiagnostics(diagnostics: List<GouseDiagnosticSpan>): List<GouseDiagnosticSpan> {
        return diagnostics.filter { diagnostic ->
            isUnusedDiagnostic(diagnostic.description)
        }
    }

    fun hasDiagnosticAtCaretOrSelection(
        diagnostics: List<GouseDiagnosticSpan>,
        rangeStart: Int,
        rangeEnd: Int,
    ): Boolean {
        return diagnostics.any { diagnostic ->
            overlaps(
                startA = diagnostic.startOffset,
                endA = diagnostic.endOffset,
                startB = rangeStart,
                endB = rangeEnd,
            )
        }
    }

    fun hasDiagnosticsInFile(diagnostics: List<GouseDiagnosticSpan>): Boolean = diagnostics.isNotEmpty()

    fun hasGouseTodoMarker(documentText: String): Boolean = documentText.contains(GOUSE_TODO_MARKER)

    private fun overlaps(
        startA: Int,
        endA: Int,
        startB: Int,
        endB: Int,
    ): Boolean {
        if (startA >= endA || startB >= endB) {
            return false
        }
        return startA < endB && startB < endA
    }

    private fun isUnusedDiagnostic(description: String?): Boolean {
        val normalizedDescription = description?.lowercase(Locale.ROOT) ?: return false
        return UNUSED_DIAGNOSTIC_FRAGMENTS.any(normalizedDescription::contains)
    }
}
