package com.getaround.pycharm.dbt.services

import com.getaround.pycharm.dbt.MyBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class DbtProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }

    override fun toString(): String {
        return "DbtProjectService()"
    }

    fun findDbtProjectRoot(file: VirtualFile): VirtualFile? {
        if (file.parent == null) return null
        if (!file.isDirectory) return findDbtProjectRoot(file.parent)

        if (file.children.any { child -> child.name == "dbt_project.yml" || child.name == "dbt_project.yaml" }) {
            return file
        }

        return findDbtProjectRoot(file.parent)
    }

    fun findChildRecursively(directory: VirtualFile, name: String): VirtualFile? {
        var result = directory.findChild(name)
        if (result != null) return result

        for (child in directory.children) {
            if (child.isDirectory) {
                result = findChildRecursively(child, name)
                if (result != null) return result
            }
        }

        return null
    }

    fun findReference(name: String, file: VirtualFile): VirtualFile? {
        val findDbtProjectRoot = findDbtProjectRoot(file)
        val projectRoot = findDbtProjectRoot ?: return null
        return findChildRecursively(projectRoot, "$name.sql")
    }
}
