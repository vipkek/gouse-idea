package com.looshch.gouse.idea.intentions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GouseDiagnosticMatcherTest {
    @Test
    fun `filters declared-and-not-used diagnostics`() {
        val filtered = GouseDiagnosticMatcher.filterUnusedDiagnostics(
            listOf(
                GouseDiagnosticSpan(10, 20, "unused declared and not used"),
                GouseDiagnosticSpan(30, 40, "another warning"),
            ),
        )

        assertEquals(1, filtered.size)
        assertEquals(10, filtered.first().startOffset)
    }

    @Test
    fun `filters unused-variable diagnostics case-insensitively`() {
        val filtered = GouseDiagnosticMatcher.filterUnusedDiagnostics(
            listOf(
                GouseDiagnosticSpan(10, 20, "Unused variable 'value'"),
                GouseDiagnosticSpan(30, 40, "another warning"),
            ),
        )

        assertEquals(1, filtered.size)
        assertEquals(10, filtered.first().startOffset)
    }

    @Test
    fun `matches diagnostic at caret range`() {
        val hasMatch = GouseDiagnosticMatcher.hasDiagnosticAtCaretOrSelection(
            diagnostics = listOf(GouseDiagnosticSpan(10, 20, "unused declared and not used")),
            rangeStart = 15,
            rangeEnd = 16,
        )

        assertTrue(hasMatch)
    }

    @Test
    fun `does not match when caret range is outside diagnostics`() {
        val hasMatch = GouseDiagnosticMatcher.hasDiagnosticAtCaretOrSelection(
            diagnostics = listOf(GouseDiagnosticSpan(10, 20, "unused declared and not used")),
            rangeStart = 30,
            rangeEnd = 31,
        )

        assertFalse(hasMatch)
    }

    @Test
    fun `reports presence of any diagnostics in file`() {
        assertTrue(
            GouseDiagnosticMatcher.hasDiagnosticsInFile(
                listOf(GouseDiagnosticSpan(1, 2, "unused declared and not used")),
            ),
        )
        assertFalse(GouseDiagnosticMatcher.hasDiagnosticsInFile(emptyList()))
    }

    @Test
    fun `detects gouse todo marker in document`() {
        assertTrue(
            GouseDiagnosticMatcher.hasGouseTodoMarker(
                "package main\n/* TODO: gouse */\nfunc main() {}\n",
            ),
        )
        assertFalse(
            GouseDiagnosticMatcher.hasGouseTodoMarker(
                "package main\n// TODO: gouse\nfunc main() {}\n",
            ),
        )
    }
}
