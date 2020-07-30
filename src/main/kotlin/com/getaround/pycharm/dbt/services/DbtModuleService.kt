package com.getaround.pycharm.dbt.services

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FilenameIndex

class DbtModuleService(private val module: Module) {
    fun findReferences(refName: String): Array<PsiFile> {
        val moduleScope = module.getModuleScope(false)
        val filesByName = FilenameIndex.getFilesByName(module.project, "$refName.sql", moduleScope)
        println("getFilesByName(${refName}.sql, $moduleScope) -> $filesByName")
        return filesByName
    }

    init {
        println(module.moduleFilePath)
    }
}