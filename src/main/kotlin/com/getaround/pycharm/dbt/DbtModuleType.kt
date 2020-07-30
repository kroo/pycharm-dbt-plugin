package com.getaround.pycharm.dbt

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import icons.PythonSdkIcons
import javax.swing.Icon

class DbtModuleType : ModuleType<DbtModuleBuilder>(DBT_MODULE) {
    override fun createModuleBuilder(): DbtModuleBuilder {
        return DbtModuleBuilder()
    }

    override fun getName(): String {
        return "DBT Project Module"
    }

    override fun getDescription(): String {
        return "A dbt project root, as designated by a dbt_project.yml file."
    }

    override fun getNodeIcon(isOpened: Boolean): Icon {
        return PythonSdkIcons.Python
    }

    companion object {
        const val DBT_MODULE = "DBT_MODULE"

        fun getInstance(): DbtModuleType? {
            return ModuleTypeManager.getInstance().findByID(DBT_MODULE) as DbtModuleType
        }
    }
}

class DbtModuleBuilder : ModuleBuilder() {
    override fun getModuleType(): DbtModuleType? {
        return DbtModuleType.getInstance()
    }
}
