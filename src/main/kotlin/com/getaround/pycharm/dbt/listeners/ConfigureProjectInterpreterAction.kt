package com.getaround.pycharm.dbt.listeners

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.configuration.PythonSdkDetailsDialog

class ConfigureProjectInterpreterAction(
    val project: Project,
    private val actionText: String = "Configure Project Interpreter",
    private val callback: (t: Sdk?) -> Unit = {}
) :
        AnAction(actionText) {

    override fun update(e: AnActionEvent) {
        e.presentation.text = actionText
        e.presentation.description = actionText
        e.presentation.isEnabledAndVisible = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        PythonSdkDetailsDialog(project, null, {}, {}).showAndGet()
    }
}
