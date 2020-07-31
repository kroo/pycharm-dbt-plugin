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

    override fun toString(): String {
        return "DbtProjectService()"
    }

    fun dbtModuleForProjectFile(dbtProjectFile: VirtualFile): DbtModule? {
        assertEquals("dbt_project.yml", dbtProjectFile.name)

        val module = modules[dbtProjectFile.canonicalFile]
        if (module != null) {
            return module
        } else {
            val psiFile = PsiManager.getInstance(project).findFile(dbtProjectFile) ?: return null
            modules[dbtProjectFile] = DbtModule(psiFile)
            return modules[dbtProjectFile]
        }
    }

    /**
     * Look up the containing dbt project module for a particular file
     */
    fun findDbtProjectModule(file: VirtualFile): DbtModule? {
        if (file.parent == null) return null
        if (!file.isDirectory) return findDbtProjectModule(file.parent)

        val dbtProjectFile = file.findChild("dbt_project.yml")
        if (dbtProjectFile != null) {
            return dbtModuleForProjectFile(dbtProjectFile)
        }

        return findDbtProjectModule(file.parent)
    }

    /**
     * Look up the containing dbt project module for a particular psiFile
     */
    fun findDbtProjectModule(psiFile: PsiFile): DbtModule? {
        var vf = psiFile.virtualFile ?: psiFile.originalFile.virtualFile ?: return null
        return findDbtProjectModule(vf)
    }
}
