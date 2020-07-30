package com.getaround.pycharm.dbt.listeners

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

internal class DbtProjectManagerListener : ProjectManagerListener {

    override fun projectOpened(project: Project) {
//        val mmgr = ModuleManager.getInstance(project)

//        val projScope = ProjectScope.getProjectScope(project)
//        val moduleFiles = FilenameIndex.getFilesByName(project, "dbt_project.yml", projScope)
//        for (moduleFile in moduleFiles) {
//            val dbtModule = DbtModule(moduleFile)
//            val modifiableModel = mmgr.modifiableModel
//            val module = modifiableModel.newNonPersistentModule(dbtModule.containingDirectory().name, DbtModuleType.DBT_MODULE)
//            ApplicationManager.getApplication().runWriteAction {
//                modifiableModel.commit()
//            }
//            val rm = ModuleRootManager.getInstance(module).modifiableModel
//            rm.addContentEntry(dbtModule.containingDirectory().virtualFile)
//            ApplicationManager.getApplication().runWriteAction {
//                rm.commit()
//            }
//        }
    }
}