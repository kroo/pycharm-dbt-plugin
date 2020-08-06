package com.getaround.pycharm.dbt.docs

import com.getaround.pycharm.dbt.completion.DbtJinja2BuiltinFunctionFakePsiElement
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.psi.PsiElement
import com.jetbrains.python.documentation.PyDocumentationBuilder

class DbtDocumentationProvider : DocumentationProvider {
    override fun getUrlFor(element: PsiElement?, originalElement: PsiElement?): MutableList<String> {
        if (element is DbtJinja2BuiltinFunctionFakePsiElement) {
            val documentationLink = element.function.documentationLink
            return if (documentationLink != null) mutableListOf(documentationLink) else mutableListOf()
        }

        return mutableListOf()
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element is DbtJinja2BuiltinFunctionFakePsiElement) {
            return PyDocumentationBuilder(element.parent, originalElement).build()
        }
        return null
    }
}
