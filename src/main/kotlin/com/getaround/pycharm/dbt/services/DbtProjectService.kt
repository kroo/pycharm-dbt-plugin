package com.getaround.pycharm.dbt.services

import com.getaround.pycharm.dbt.DbtPluginBundle
import com.getaround.pycharm.dbt.module.DbtModule
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import kotlin.test.assertEquals

class DbtProjectService(val project: Project) {
    private val modules = HashMap<VirtualFile, DbtModule>()

    init {
        println(DbtPluginBundle.message("projectService", project.name))
    }

    private fun dbtModuleForProjectFile(dbtProjectFile: VirtualFile): DbtModule? {
        assertEquals("dbt_project.yml", dbtProjectFile.name)

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
    private fun findDbtProjectModule(file: VirtualFile): DbtModule? {
        val dbtProjectFile = file.findChild("dbt_project.yml")

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
}
