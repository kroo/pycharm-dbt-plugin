package com.getaround.pycharm.dbt.run

import com.getaround.pycharm.dbt.services.DbtProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.selectedValueIs
import org.jetbrains.annotations.NotNull
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JComponent

data class DbtUiSettings(
        private var _projectRoot: String,
        private var _command: String,
        private var _args: String) {

    var projectRoot: String
        get() = _projectRoot
        set(value) {
            _projectRoot = value
        }

    var command: String
        get() = _command
        set(value) {
            _command = value
        }

    var args: String
        get() = _args
        set(value) {
            _args = value
        }

}

class DbtSettingsEditor(private val project: @NotNull Project) : SettingsEditor<DbtRunConfiguration>() {
    private val uiSettings = DbtUiSettings("", "", "")
    private var projectRootJC: ComboBox<String>? = null
    private var commandJC: ComboBox<String>? = null
    private var argsJC: JBTextField? = null

    override fun resetEditorFrom(s: DbtRunConfiguration) {
        uiSettings.projectRoot = s.projectRoot
        uiSettings.command = s.command
        uiSettings.args = s.args

        projectRootJC?.item = uiSettings.projectRoot
        commandJC?.item = uiSettings.command
        argsJC?.text = uiSettings.args
    }

    override fun createEditor(): JComponent {
        return panel {
            row("Project Root") {
                projectRootJC = comboBox(DefaultComboBoxModel<String>(getDbtProjectRoots().toTypedArray()), uiSettings::projectRoot)
                        .withErrorOnApplyIf("Must not be blank!") { it.selectedItem == "" }
                        .constraints(growX).component

                if (projectRootJC is ComboBox<String>) {
                    projectRootJC!!.addActionListener { uiSettings.projectRoot = projectRootJC!!.item }
                    projectRootJC!!.item = uiSettings.projectRoot

                }
            }
            row("DBT Command") {
                commandJC = comboBox(DefaultComboBoxModel<String>(DBT_COMMANDS), uiSettings::command)
                        .constraints(growX).component

                commandJC!!.addActionListener { uiSettings.command = commandJC!!.item }
                commandJC!!.item = uiSettings.command
            }
            row("Arguments") {
                argsJC = textField(uiSettings::args)
                        .constraints(growX).component

                argsJC!!.addActionListener { uiSettings.args = argsJC!!.text }
                argsJC!!.text = uiSettings.args
            }
        }
    }

    private fun getDbtProjectRoots(): List<String> {
        val dbtService = project.service<DbtProjectService>()
        val findAllDbtModules = dbtService.findAllDbtModules()
        val dbtModulePaths = findAllDbtModules.map { it.projectYmlFile.containingDirectory.virtualFile.path }.toMutableList()
        dbtModulePaths.add(0, "")
        return dbtModulePaths
    }

    override fun applyEditorTo(s: DbtRunConfiguration) {
        s.projectRoot = uiSettings.projectRoot
        s.command = uiSettings.command
        s.args = uiSettings.args
    }

    companion object {
        private val DBT_COMMANDS = arrayOf(
                "",
                "compile",
                "run",
                "test",
                "deps",
                "snapshot",
                "clean",
                "seed",
                "source",
                "run-operation"
        )
    }

}