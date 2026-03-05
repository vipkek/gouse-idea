package com.looshch.gouse.idea.run

import com.intellij.testFramework.LightVirtualFile
import kotlin.test.Test
import kotlin.test.assertEquals

class GouseTargetTest {
    @Test
    fun `returns open-go-file error when file is missing`() {
        assertEquals("error.open.go.file", GouseTarget.validationError(null))
    }

    @Test
    fun `returns only-go-files error for non-go file extension`() {
        val file = LightVirtualFile("example.txt", "")

        assertEquals("error.only.go.files", GouseTarget.validationError(file))
    }

    @Test
    fun `returns only-local-files error for non-local go file`() {
        val file = LightVirtualFile("example.go", "package main")

        assertEquals("error.only.local.files", GouseTarget.validationError(file))
    }
}
