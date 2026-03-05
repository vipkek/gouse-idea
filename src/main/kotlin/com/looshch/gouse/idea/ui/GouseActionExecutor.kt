package com.looshch.gouse.idea.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.looshch.gouse.idea.GouseBundle
import com.looshch.gouse.idea.run.RunResult
import com.looshch.gouse.idea.services.GouseCommandService

object GouseActionExecutor {
    private val notifier = GouseNotifier()

    fun execute(project: Project, virtualFile: VirtualFile) {
        runInBackground(
            project = project,
            titleKey = "task.run.title",
            work = {
                GouseCommandService.getInstance().runOnFile(project, virtualFile)
            },
            onDone = { result ->
                handleRunResult(project, virtualFile, result, afterInstall = false)
            },
        )
    }

    private fun installAndRun(project: Project, virtualFile: VirtualFile) {
        runInBackground(
            project = project,
            titleKey = "task.install.title",
            work = {
                GouseCommandService.getInstance().installAndRun(project, virtualFile)
            },
            onDone = { result ->
                handleRunResult(project, virtualFile, result, afterInstall = true)
            },
        )
    }

    private fun runInBackground(
        project: Project,
        titleKey: String,
        work: () -> RunResult,
        onDone: (RunResult) -> Unit,
    ) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, GouseBundle.message(titleKey), false) {
            override fun run(indicator: ProgressIndicator) {
                val result = work()
                ApplicationManager.getApplication().invokeLater {
                    onDone(result)
                }
            }
        })
    }

    private fun handleRunResult(
        project: Project,
        virtualFile: VirtualFile,
        result: RunResult,
        afterInstall: Boolean,
    ) {
        when (result) {
            is RunResult.Success -> Unit
            is RunResult.InvalidTarget -> notifier.notifyWarning(project, result.reason)
            is RunResult.ConfiguredPathMissing -> notifier.notifyConfiguredPathMissing(project, result.path)
            is RunResult.GouseMissing -> {
                if (afterInstall) {
                    notifier.notifyInstalledButUnreachable(project)
                } else {
                    notifier.notifyMissingGouse(project) {
                        installAndRun(project, virtualFile)
                    }
                }
            }
            is RunResult.GoMissing -> notifier.notifyGoMissing(project)
            is RunResult.ExecutionFailed -> notifier.notifyError(
                project,
                if (afterInstall) result.message else GouseBundle.message("error.gouse.failed", result.message),
            )
            is RunResult.RefreshFailed -> notifier.notifyWarning(
                project,
                GouseBundle.message("error.refresh.failed", result.message),
            )
        }
    }
}
