package com.getaround.pycharm.dbt

import com.getaround.pycharm.dbt.services.DbtProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.jetbrains.extensions.python.toPsi


class DbtRefReference(element: PsiElement, textRange: TextRange) : PsiReferenceBase<PsiElement>(element, textRange) {
    val refName = element.text.substring(textRange.startOffset, textRange.endOffset)
    val projectService = element.project.service<DbtProjectService>()
//    val moduleService = moduleForFile(element)?.getService(DbtModuleService::class.java)

//    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
//        val results: MutableList<ResolveResult> = ArrayList()
//        moduleService?.findReferences(refName)?.forEach { file ->
//            results.add(PsiElementResolveResult(file))
//        }
//        return results.toTypedArray()
//    }

    override fun resolve(): PsiElement? {
//        val resolveResults = moduleService?.findReferences(refName)
//        println("$moduleService.findReferences($refName) resulted in $resolveResults")
//        if (resolveResults != null || resolveResults?.size == 1) {
//            return resolveResults[0]
//        }
//        return null

        return projectService
                .findReference(refName, element.containingFile.virtualFile)
                ?.toPsi(element.project)
    }

//    private fun moduleForFile(psiElement: PsiElement): Module? {
//        return ProjectRootManager
//                .getInstance(element.project)
//                .getFileIndex()
//                .getModuleForFile(psiElement.getContainingFile().getVirtualFile())
//    }
}