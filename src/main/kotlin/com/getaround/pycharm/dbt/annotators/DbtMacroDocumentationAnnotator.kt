package com.getaround.pycharm.dbt.annotators

import com.getaround.pycharm.dbt.services.DbtProjectService
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.jetbrains.django.lang.template.psi.impl.DjangoNamedExpressionImpl
import com.jetbrains.jinja2.tags.Jinja2MacroParameter
import com.jetbrains.jinja2.tags.Jinja2MacroTag
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.impl.YAMLMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLSequenceItemImpl

class DbtMacroDocumentationAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val macroTag: Jinja2MacroTag
        var macroParameter: Jinja2MacroParameter? = null
        if (element.parent?.parent is Jinja2MacroTag && element.parent is DjangoNamedExpressionImpl) {
            macroTag = element.parent?.parent as Jinja2MacroTag
        } else if (element.parent?.parent is Jinja2MacroParameter && element.parent is DjangoNamedExpressionImpl) {
            macroTag = element.parent?.parent?.parent as Jinja2MacroTag
            macroParameter = element.parent?.parent as Jinja2MacroParameter?
        } else {
            return
        }

        val macroTagName = macroTag.name ?: return
        val service = element.project.service<DbtProjectService>()
        val module = service.findDbtProjectModule(element.containingFile)
        val macroProperties: YAMLSequenceItemImpl? = module?.findMacroProperties(macroTagName) as YAMLSequenceItemImpl?
        val containsDescription = macroProperties?.keysValues.orEmpty().any { it.keyText == "description" }
        val containsParameter = macroParameter != null && (
                macroProperties
                        ?.keysValues.orEmpty()
                        .firstOrNull { it.keyText == "arguments" }
                        ?.value as YAMLSequence?
                )?.items?.firstOrNull {
                    it.keysValues.any { kv ->
                        kv.keyText == "name" && kv.valueText == macroParameter.nameElement?.name
                    } && (it.children.first() as YAMLMappingImpl?)?.getKeyValueByKey("description") != null
                } != null
        if (macroParameter == null && (macroProperties == null || !containsDescription)) {
            holder
                    .newAnnotation(HighlightSeverity.WEAK_WARNING, "Missing documentation for macro")
                    .range(element.textRange)
                    .create()
        } else if (macroParameter != null && (macroProperties == null || !containsParameter)) {
            holder
                    .newAnnotation(HighlightSeverity.WEAK_WARNING, "Missing documentation for parameter")
                    .range(element.textRange)
                    .create()
        } else {
            return
        }
    }
}
