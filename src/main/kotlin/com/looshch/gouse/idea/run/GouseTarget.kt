package com.looshch.gouse.idea.run

import com.intellij.openapi.vfs.VirtualFile

internal object GouseTarget {
    fun validationError(file: VirtualFile?): String? {
        if (file == null) {
            return "error.open.go.file"
        }
        if (file.isDirectory || file.extension?.lowercase() != "go") {
            return "error.only.go.files"
        }
        if (!file.isInLocalFileSystem) {
            return "error.only.local.files"
        }

        return null
    }

    fun isValid(file: VirtualFile?): Boolean = validationError(file) == null
}
