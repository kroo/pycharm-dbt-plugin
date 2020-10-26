package com.getaround.pycharm.dbt.run

import com.getaround.pycharm.dbt.DbtIcons
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeBase
import javax.swing.Icon

class DbtRunConfigurationType : ConfigurationTypeBase(
        "DBT", "DBT", "dbt command", DbtIcons.DBT_ICON) {
    init {
        addFactory(DbtRunConfigurationFactory(this))
    }

    companion object {
        val INSTANCE = DbtRunConfigurationType()
    }
}