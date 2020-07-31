package com.getaround.pycharm.dbt

import com.getaround.pycharm.dbt.services.DbtProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase


class DbtRefReference(element: PsiElement, textRange: TextRange) : PsiReferenceBase<PsiElement>(element, textRange) {
    private val refName = element.text.substring(textRange.startOffset, textRange.endOffset)
    private val projectService = element.project.service<DbtProjectService>()

    override fun resolve(): PsiElement? {
        return projectService
                .findDbtProjectModule(element.containingFile)
                ?.findModel(refName)
    }
}