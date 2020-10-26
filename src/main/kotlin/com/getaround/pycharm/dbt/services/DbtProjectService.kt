package com.getaround.pycharm.dbt.services

import com.getaround.pycharm.dbt.DbtPluginBundle
import com.getaround.pycharm.dbt.module.DbtModule
import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import kotlin.test.assertEquals

class DbtProjectService(val project: Project) {
    private val modules = HashMap<VirtualFile, DbtModule>()
    private val notificationGroup = NotificationGroup("DBT Messages", NotificationDisplayType.BALLOON, true)

    init {
        println(DbtPluginBundle.message("projectService", project.name))
    }

    fun findAllDbtModules(): List<DbtModule> {
        return FilenameIndex
                .getFilesByName(project, DBT_PROJECT_FILENAME,
                        GlobalSearchScope.projectScope(project))
                .mapNotNull {
                    dbtModuleForProjectFile(it.virtualFile)
                }
    }

    private fun dbtModuleForProjectFile(dbtProjectFile: VirtualFile): DbtModule? {
        assertEquals(DBT_PROJECT_FILENAME, dbtProjectFile.name)

        var module = modules[dbtProjectFile.canonicalFile]
        if (module != null) {
            return module
        }

        val psiFile = PsiManager.getInstance(project).findFile(dbtProjectFile)
        if (psiFile != null) {
            module = DbtModule(psiFile)
            modules[dbtProjectFile] = module
        }
        return module
    }

    /**
     * Look up the containing dbt project module for a particular file
     */
    fun findDbtProjectModule(file: VirtualFile): DbtModule? {
        val dbtProjectFile = file.findChild(DBT_PROJECT_FILENAME)

        return if (file.parent == null) null
        else if (!file.isDirectory) findDbtProjectModule(file.parent)
        else if (dbtProjectFile != null) dbtModuleForProjectFile(dbtProjectFile)
        else findDbtProjectModule(file.parent)
    }

    /**
     * Look up the containing dbt project module for a particular psiFile
     */
    fun findDbtProjectModule(psiFile: PsiFile): DbtModule? {
        val vf = psiFile.virtualFile ?: psiFile.originalFile.virtualFile ?: return null
        return findDbtProjectModule(vf)
    }

    fun notifyInfo(content: String): Notification {
        val notification = notificationGroup.createNotification(
                "DBT: $content", NotificationType.INFORMATION)
        notification.notify(project)
        return notification
    }

    fun notifyError(content: String): Notification {
        val notification = notificationGroup.createNotification(
                "DBT: $content", NotificationType.ERROR)
        notification.notify(project)
        return notification
    }

    companion object {
        const val DBT_PROJECT_FILENAME = "dbt_project.yml"
    }
}
