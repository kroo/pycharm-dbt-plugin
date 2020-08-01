package com.getaround.pycharm.dbt.references

import com.getaround.pycharm.dbt.completion.DbtSourceSchemaCompletionLookup
import com.getaround.pycharm.dbt.services.DbtProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase


class DbtSourceSchemaReference(
        element: PsiElement, textRange: TextRange) : PsiReferenceBase<PsiElement>(element, textRange) {
    private val sourceName = element.text.substring(textRange.startOffset, textRange.endOffset)
    private val projectService = element.project.service<DbtProjectService>()

    override fun resolve(): PsiElement? {
        return projectService
                .findDbtProjectModule(element.containingFile)
                ?.findSourceSchema(sourceName)
    }

    override fun getVariants(): Array<DbtSourceSchemaCompletionLookup> {
        return projectService
                .findDbtProjectModule(element.containingFile)
                ?.findAllSourceSchemas()
                ?.map { DbtSourceSchemaCompletionLookup(it) }
                ?.toTypedArray()
                ?: arrayOf()
    }
}