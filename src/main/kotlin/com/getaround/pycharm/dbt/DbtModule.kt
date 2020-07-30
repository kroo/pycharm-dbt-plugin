package com.getaround.pycharm.dbt

import com.intellij.psi.PsiFile

class DbtModule(val ymlFile: PsiFile) {
    override fun toString(): String {
        return "DbtModule(${containingDirectory().name})"
    }

    fun containingDirectory() = ymlFile.containingDirectory
}
