package com.looshch.gouse.idea.startup

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.looshch.gouse.idea.GouseBundle
import com.looshch.gouse.idea.services.GouseCommandService
import com.looshch.gouse.idea.ui.GouseNotifier
import java.util.concurrent.atomic.AtomicBoolean

class GouseStartupActivity : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        if (!HAS_RUN.compareAndSet(false, true)) {
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            val warningMessage = try {
                GouseCommandService.getInstance().autoUpdateOnStartup()
            } catch (error: Exception) {
                GouseBundle.message(
                    "error.auto.update.failed",
                    error.message ?: "Unknown error",
                )
            }

            if (warningMessage == null || project.isDisposed) {
                return@executeOnPooledThread
            }

            ApplicationManager.getApplication().invokeLater {
                if (!project.isDisposed) {
                    GouseNotifier().notifyWarning(project, warningMessage)
                }
            }
        }
    }

    companion object {
        private val HAS_RUN = AtomicBoolean(false)
    }
}
