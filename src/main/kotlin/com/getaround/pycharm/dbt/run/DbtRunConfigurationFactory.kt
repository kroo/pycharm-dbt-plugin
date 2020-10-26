package com.getaround.pycharm.dbt.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.NotNull

class DbtRunConfigurationFactory(type: @NotNull ConfigurationType) : ConfigurationFactory(type) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return DbtRunConfiguration(project, this, "Unnamed dbt command")
    }

    override fun getId(): String {
        return FACTORY_ID
    }

    override fun getName(): String {
        return FACTORY_NAME
    }

    companion object {
        private const val FACTORY_ID = "DBT_RUN_FACTORY"
        private const val FACTORY_NAME = "DBT configuration factory"
    }
}