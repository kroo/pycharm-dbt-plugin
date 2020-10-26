package com.getaround.pycharm.dbt.docs

import com.getaround.pycharm.dbt.completion.DbtContextValueFakePsiElement
import com.getaround.pycharm.dbt.completion.DbtJinja2BuiltinFunctionFakePsiElement
import com.getaround.pycharm.dbt.module.DbtModule
import com.getaround.pycharm.dbt.services.DbtProjectService
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.util.LocalTimeCounter
import com.jetbrains.jinja2.tags.Jinja2MacroTag
import com.jetbrains.python.documentation.PyDocumentationLink.elementForLink
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.python.pyi.PyiFileType

class DbtDocumentationProvider : DocumentationProvider {
    override fun getUrlFor(element: PsiElement?, originalElement: PsiElement?): MutableList<String> {
        if (element is DbtJinja2BuiltinFunctionFakePsiElement) {
            val documentationLink = element.function.externalDocUrl
            return if (documentationLink != null) mutableListOf(documentationLink) else mutableListOf()
        }

        return mutableListOf()
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        when (element) {
            is DbtJinja2BuiltinFunctionFakePsiElement ->
                return element.function.renderMarkdownDoc(element)
            is DbtContextValueFakePsiElement ->
                return element.value.renderMarkdownDoc(element)
            is Jinja2MacroTag -> {
                val macroName = element.nameElement?.name ?: return null
                val service: DbtProjectService = element.project.service<DbtProjectService>()
                val dbtProj: DbtModule? = service.findDbtProjectModule(element.containingFile)
                val findMacro = dbtProj?.findMacro(macroName)

                return findMacro?.renderMarkdownDoc(element)
            }
            else -> return null
        }
    }

    override fun generateHoverDoc(element: PsiElement, originalElement: PsiElement?): String? {
        if (element is DbtJinja2BuiltinFunctionFakePsiElement) {
            return element.function.renderLinkedDefinitionDocContent(element)
        }
        return null
    }

    override fun getDocumentationElementForLink(psiManager: PsiManager?, link: String?, context: PsiElement?): PsiElement? {
        if (link == null) return null
        if (context == null) return null

        if (
                link == PYLINK_TYPE_CLASS ||
                link == PYLINK_TYPE_PARAM ||
                link.startsWith(PYLINK_TYPE_TYPENAME) ||
                link.startsWith(PYLINK_TYPE_FUNC) ||
                link.startsWith(PYLINK_TYPE_MODULE)) {
            val psiFileFactory = PsiFileFactory.getInstance(context.project)
            val pythonFileType = PyiFileType.INSTANCE
            val value = LocalTimeCounter.currentTime()
            val tempFileName = "temp.pyi"
            val fileContents = "from typing import *"
            val pyFile: PyFile = psiFileFactory.createFileFromText(tempFileName, pythonFileType, fileContents, value, true) as PyFile
            val pyContext = pyFile.children.first()
            return elementForLink(link, pyContext, TypeEvalContext.deepCodeInsight(context.project))
        }

        return null
    }

    companion object {
        private const val PYLINK_TYPE_CLASS = "#class#"
        private const val PYLINK_TYPE_PARAM = "#param#"
        private const val PYLINK_TYPE_TYPENAME = "#typename#"
        private const val PYLINK_TYPE_FUNC = "#func#"
        private const val PYLINK_TYPE_MODULE = "#module#"
    }
}
