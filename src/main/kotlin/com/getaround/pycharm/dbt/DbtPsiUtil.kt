package com.getaround.pycharm.dbt

import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile

object DbtPsiUtil {
    fun isChildOf(directory: PsiDirectory, file: PsiFile): Boolean {
        var parent = file.parent
        while (parent != null) {
            if (directory == parent) {
                return true
            }
            parent = parent.parentDirectory
        }
        return false
    }
}
