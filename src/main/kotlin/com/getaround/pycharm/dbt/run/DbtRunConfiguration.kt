package com.getaround.pycharm.dbt.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.psi.PsiFile
import com.jetbrains.python.run.AbstractPythonRunConfiguration
import org.jdom.Element

class DbtRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
        AbstractPythonRunConfiguration<DbtRunConfiguration>(project, factory) {

    var projectRoot: String = ""
    var command: String = ""
    var args: String = ""
    var constructedFromFilePath: String? = null

    init {
        this.name = name
    }

    override fun getActionName(): String? {
        return name
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        return DbtRunConfigurationEnvironment(this, environment)
    }

    override fun createConfigurationEditor(): SettingsEditor<DbtRunConfiguration> {
        return DbtSettingsEditor(project)
    }

    override fun writeExternal(element: Element) {
        JDOMExternalizerUtil.writeField(element, DBT_PROJECT_ROOT, projectRoot)
        JDOMExternalizerUtil.writeField(element, DBT_COMMAND, command)
        JDOMExternalizerUtil.writeField(element, DBT_ARGS, args)
        JDOMExternalizerUtil.writeField(element, DBT_CONSTRUCTED_FROM_PATH, constructedFromFilePath.orEmpty())
        super.writeExternal(element)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        projectRoot = JDOMExternalizerUtil.readField(element, DBT_PROJECT_ROOT).orEmpty()
        command = JDOMExternalizerUtil.readField(element, DBT_COMMAND).orEmpty()
        args = JDOMExternalizerUtil.readField(element, DBT_ARGS).orEmpty()
        constructedFromFilePath = JDOMExternalizerUtil.readField(element, DBT_CONSTRUCTED_FROM_PATH)
        if (constructedFromFilePath.isNullOrEmpty()) {
            constructedFromFilePath = null
        }
    }

    companion object {
        private const val DBT_PROJECT_ROOT = "DBT_PROJECT_ROOT"
        private const val DBT_COMMAND = "DBT_COMMAND"
        private const val DBT_ARGS = "DBT_ARGS"
        private const val DBT_CONSTRUCTED_FROM_PATH = "DBT_CONSTRUCTED_FROM_PATH"
    }
}