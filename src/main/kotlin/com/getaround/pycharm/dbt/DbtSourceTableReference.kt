package com.getaround.pycharm.dbt

import com.getaround.pycharm.dbt.services.DbtProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.jetbrains.django.completion.DjangoItemCompletionLookup


class DbtSourceTableReference(
        element: PsiElement, textRange: TextRange,
        sourceElement: PsiElement, sourceTextRange: TextRange,
        tableElement: PsiElement, tableTextRange: TextRange) : PsiReferenceBase<PsiElement>(element, textRange) {
    private val sourceName = sourceElement.text.substring(sourceTextRange.startOffset, sourceTextRange.endOffset)
    private val tableName = tableElement.text.substring(tableTextRange.startOffset, tableTextRange.endOffset)
    private val projectService = sourceElement.project.service<DbtProjectService>()

    override fun resolve(): PsiElement? {
        return projectService
                .findDbtProjectModule(element.containingFile)
                ?.findSourceTable(sourceName, tableName)
    }

    override fun getVariants(): Array<DjangoItemCompletionLookup> {
        return projectService
                .findDbtProjectModule(element.containingFile)
                ?.findAllSourceTables(sourceName)
                ?.map { DjangoItemCompletionLookup(it) }
                ?.toTypedArray()
                ?: arrayOf()
    }


}