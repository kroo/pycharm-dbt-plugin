package com.getaround.pycharm.dbt.run

import com.getaround.pycharm.dbt.services.DbtProjectService
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

class DbtRunConfigurationProducer : RunConfigurationProducer<DbtRunConfiguration>(false) {
    override fun getConfigurationFactory(): ConfigurationFactory {
        return DbtRunConfigurationFactory(DbtRunConfigurationType.INSTANCE)
    }

    override fun isConfigurationFromContext(configuration: DbtRunConfiguration, context: ConfigurationContext): Boolean {
        val containingFile = context.location?.virtualFile
        if (containingFile?.extension != "sql") {
            return false
        }
        return configuration.constructedFromFilePath == containingFile.path
    }

    override fun setupConfigurationFromContext(configuration: DbtRunConfiguration, context: ConfigurationContext, sourceElement: Ref<PsiElement>): Boolean {
        val containingFile = context.location?.virtualFile
        if (containingFile?.extension != "sql") {
            return false
        }

        val dbtService = context.project.service<DbtProjectService>()
        val dbtProject = dbtService.findDbtProjectModule(containingFile)
        val virtualFile = dbtProject?.projectRoot?.virtualFile ?: return false

        configuration.projectRoot = virtualFile.path
        configuration.constructedFromFilePath = containingFile.path

        val name = containingFile.nameWithoutExtension
        when {
            dbtProject.isModelFile(containingFile) -> {
                configuration.name = "dbt run -m $name"
                configuration.command = "run"
                configuration.args = "-m $name"
            }
            dbtProject.isTestFile(containingFile) -> {
                configuration.name = "dbt test -m $name"
                configuration.command = "test"
                configuration.args = "-m $name"
            }
            else -> {
                configuration.name = "dbt test -m $name"
                configuration.command = "compile"
                configuration.args = ""
            }
        }

        return true
    }
}