package com.looshch.gouse.idea.intentions

import com.goide.psi.GoFile
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.looshch.gouse.idea.GouseBundle
import com.looshch.gouse.idea.run.GouseTarget
import com.looshch.gouse.idea.ui.GouseActionExecutor

class ApplyGouseIntention : PsiElementBaseIntentionAction(), DumbAware {
    override fun getText(): String = GouseBundle.message("action.toggle.text")

    override fun getFamilyName(): String = GouseBundle.message("intention.family")

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val psiFile = element.containingFile
        if (psiFile !is GoFile) {
            return false
        }

        val file = editor?.virtualFile ?: element.containingFile?.virtualFile
        if (!GouseTarget.isValid(file)) {
            return false
        }

        if (editor == null) {
            return false
        }

        if (GouseDiagnosticMatcher.hasGouseTodoMarker(editor.document.text)) {
            return true
        }

        val selectionModel = editor.selectionModel
        val rangeStart =
            if (selectionModel.hasSelection()) {
                selectionModel.selectionStart
            } else {
                editor.caretModel.offset
            }
        val rangeEnd =
            if (selectionModel.hasSelection()) {
                selectionModel.selectionEnd
            } else {
                editor.caretModel.offset + 1
            }

        val diagnostics = GouseDiagnosticMatcher.matchingDiagnostics(project, editor)
        return GouseDiagnosticMatcher.hasDiagnosticAtCaretOrSelection(diagnostics, rangeStart, rangeEnd) ||
            GouseDiagnosticMatcher.hasDiagnosticsInFile(diagnostics)
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val file = editor?.virtualFile ?: element.containingFile?.virtualFile ?: return
        GouseActionExecutor.execute(project, file)
    }

    override fun startInWriteAction(): Boolean = false
}
