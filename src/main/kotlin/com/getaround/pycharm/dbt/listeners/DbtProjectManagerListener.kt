package com.getaround.pycharm.dbt.listeners

import com.getaround.pycharm.dbt.services.DbtProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.sql.psi.SqlLanguage
import com.jetbrains.jinja2.Jinja2Language
import com.jetbrains.python.templateLanguages.TemplatesService

internal class DbtProjectManagerListener : ProjectManagerListener {

    override fun projectOpened(project: Project) {
        val instance = project.service<DbtProjectService>()
        instance.findAllDbtModules().forEach {
            val module = ModuleUtil.findModuleForFile(it.projectYmlFile)
            val templatesService = TemplatesService.getInstance(module)
            templatesService.templateLanguage = Jinja2Language.INSTANCE.templateLanguageName
            templatesService.templateFileTypes = listOf(SqlLanguage.INSTANCE.associatedFileType?.name)
        }
    }
}
