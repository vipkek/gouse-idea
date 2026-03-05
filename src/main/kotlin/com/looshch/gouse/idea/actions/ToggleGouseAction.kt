package com.looshch.gouse.idea.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.looshch.gouse.idea.GouseBundle
import com.looshch.gouse.idea.run.GouseTarget
import com.looshch.gouse.idea.ui.GouseActionExecutor
import com.looshch.gouse.idea.ui.GouseNotifier

class ToggleGouseAction : DumbAwareAction(
    GouseBundle.message("action.toggle.text"),
    GouseBundle.message("action.toggle.description"),
    null,
) {
    override fun update(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        event.presentation.isEnabledAndVisible = GouseTarget.isValid(file)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val errorKey = GouseTarget.validationError(file)
        if (errorKey != null) {
            GouseNotifier().notifyWarning(project, GouseBundle.message(errorKey))
            return
        }

        if (project == null || file == null) {
            return
        }

        GouseActionExecutor.execute(project, file)
    }
}
