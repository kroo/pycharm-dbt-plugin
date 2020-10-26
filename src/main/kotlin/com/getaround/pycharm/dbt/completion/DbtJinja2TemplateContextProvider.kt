package com.getaround.pycharm.dbt.completion

import com.getaround.pycharm.dbt.services.DbtProjectService
import com.getaround.pycharm.dbt.services.DbtTypeService
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.components.service
import com.intellij.psi.PsiFile
import com.jetbrains.python.templateLanguages.TemplateContextProvider

class DbtJinja2TemplateContextProvider : TemplateContextProvider {
    override fun getTemplateContext(file: PsiFile): MutableCollection<LookupElement> {
        val result = mutableListOf<LookupElement>()
        val projectService = file.project.service<DbtProjectService>()
        val module = projectService.findDbtProjectModule(file) ?: return result

        for (fn in module.findAllDbtFunctions()) {
            result.add(DbtJinja2FunctionCompletionLookup(file, fn,
                    appendParens = true,
                    autoPopup = true))
        }

        val typeService = file.project.service<DbtTypeService>()
        for (fn in typeService.builtinValues) {
            result.add(DbtJinja2ValueCompletionLookup(file, fn, appendDot=false, autoPopup=false))
        }
        return result
    }
}
