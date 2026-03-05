package com.looshch.gouse.idea.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.util.ui.FormBuilder
import com.looshch.gouse.idea.GouseBundle
import com.looshch.gouse.idea.services.GouseSettingsService
import javax.swing.JComponent
import javax.swing.JCheckBox
import javax.swing.JPanel

class GouseConfigurable : SearchableConfigurable {
    private var executablePathField: TextFieldWithBrowseButton? = null
    private var autoUpdateOnStartupCheckbox: JCheckBox? = null

    override fun getId(): String = "com.looshch.gouse.idea.settings"

    override fun getDisplayName(): String = GouseBundle.message("configurable.display.name")

    override fun createComponent(): JComponent {
        val field = TextFieldWithBrowseButton()
        field.addBrowseFolderListener(
            GouseBundle.message("configurable.path.dialog.title"),
            null,
            null,
            FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor(),
        )
        field.text = GouseSettingsService.getInstance().getConfiguredExecutablePath()
        executablePathField = field

        val autoUpdateCheckbox = JCheckBox(GouseBundle.message("configurable.auto.update.label"))
        autoUpdateCheckbox.isSelected = GouseSettingsService.getInstance().getAutoUpdateOnStartup()
        autoUpdateOnStartupCheckbox = autoUpdateCheckbox

        return FormBuilder.createFormBuilder()
            .addLabeledComponent(GouseBundle.message("configurable.path.label"), field)
            .addComponent(autoUpdateCheckbox)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean =
        currentValue() != GouseSettingsService.getInstance().getConfiguredExecutablePath() ||
            autoUpdateOnStartupValue() != GouseSettingsService.getInstance().getAutoUpdateOnStartup()

    override fun apply() {
        GouseSettingsService.getInstance().setConfiguredExecutablePath(currentValue())
        GouseSettingsService.getInstance().setAutoUpdateOnStartup(autoUpdateOnStartupValue())
    }

    override fun reset() {
        executablePathField?.text = GouseSettingsService.getInstance().getConfiguredExecutablePath()
        autoUpdateOnStartupCheckbox?.isSelected = GouseSettingsService.getInstance().getAutoUpdateOnStartup()
    }

    override fun disposeUIResources() {
        executablePathField = null
        autoUpdateOnStartupCheckbox = null
    }

    private fun currentValue(): String = executablePathField?.text?.trim().orEmpty()

    private fun autoUpdateOnStartupValue(): Boolean = autoUpdateOnStartupCheckbox?.isSelected ?: true
}
