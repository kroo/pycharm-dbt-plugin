package com.getaround.pycharm.dbt.completion

import com.getaround.pycharm.dbt.services.DbtProjectService
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.components.service
import com.intellij.psi.PsiFile
import com.jetbrains.python.templateLanguages.TemplateContextProvider

class DbtJinja2TemplateContextProvider : TemplateContextProvider {
    override fun getTemplateContext(file: PsiFile): MutableCollection<LookupElement> {
        val result = mutableListOf<LookupElement>()
        val project = file.project.service<DbtProjectService>()
        val module = project.findDbtProjectModule(file) ?: return result

        for (fn in module.findAllDbtFunctions()) {
            result.add(DbtJinja2FunctionCompletionLookup(fn,
                    appendParens = true,
                    autoPopup = true))
        }
        return result
    }
}
