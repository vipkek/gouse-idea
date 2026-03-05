package com.looshch.gouse.idea.ui

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ide.BrowserUtil
import com.looshch.gouse.idea.GOUSE_README_URL
import com.looshch.gouse.idea.GouseBundle

class GouseNotifier {
    fun notifyWarning(project: Project?, message: String) {
        createNotification(message, NotificationType.WARNING).notify(project)
    }

    fun notifyError(project: Project?, message: String) {
        createNotification(message, NotificationType.ERROR).notify(project)
    }

    fun notifyConfiguredPathMissing(project: Project?, path: String) {
        createNotification(
            GouseBundle.message("error.configured.path", path),
            NotificationType.ERROR,
        ).addAction(openSettingsAction(project)).notify(project)
    }

    fun notifyMissingGouse(
        project: Project?,
        onInstall: () -> Unit,
    ) {
        createNotification(
            GouseBundle.message("error.gouse.missing"),
            NotificationType.ERROR,
        ).addAction(simpleAction("action.install") {
            onInstall()
        }).addAction(openReadmeAction()).notify(project)
    }

    fun notifyGoMissing(project: Project?) {
        createNotification(
            GouseBundle.message("error.go.missing"),
            NotificationType.ERROR,
        ).addAction(openReadmeAction()).notify(project)
    }

    fun notifyInstalledButUnreachable(project: Project?) {
        createNotification(
            GouseBundle.message("error.gouse.installed.but.unreachable"),
            NotificationType.ERROR,
        ).addAction(openSettingsAction(project)).notify(project)
    }

    fun openSettings(project: Project?) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "com.looshch.gouse.idea.settings")
    }

    private fun openSettingsAction(project: Project?): NotificationAction =
        simpleAction("action.open.settings") {
            openSettings(project)
        }

    private fun openReadmeAction(): NotificationAction =
        simpleAction("action.open.readme") {
            BrowserUtil.browse(GOUSE_README_URL)
        }

    private fun simpleAction(messageKey: String, handler: () -> Unit): NotificationAction =
        object : NotificationAction(GouseBundle.message(messageKey)) {
            override fun actionPerformed(event: com.intellij.openapi.actionSystem.AnActionEvent, notification: Notification) {
                notification.expire()
                handler()
            }
        }

    private fun createNotification(content: String, type: NotificationType): Notification =
        NotificationGroupManager.getInstance()
            .getNotificationGroup("gouse")
            .createNotification(GouseBundle.message("notification.title"), content, type)
}
