package com.getaround.pycharm.dbt.completion

import com.getaround.pycharm.dbt.DbtJinja2BuiltinFunction
import com.getaround.pycharm.dbt.services.DbtProjectService
import com.getaround.pycharm.dbt.services.DbtTypeService
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.components.service
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiErrorElement
import com.intellij.util.ProcessingContext
import com.jetbrains.django.lang.template.parsing.DjangoTemplateTokenTypes.ID
import com.jetbrains.django.lang.template.psi.impl.DjangoStringLiteralImpl
import com.jetbrains.django.lang.template.psi.impl.DjangoVariableReferenceImpl
import com.jetbrains.jinja2.tags.Jinja2FunctionCall

class DbtJinja2CompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(ID), DbtJinja2CompletionProvider())
    }
}

class DbtJinja2CompletionProvider : CompletionProvider<CompletionParameters?>() {
    override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            resultSet: CompletionResultSet
    ) {

        val projectService = parameters.position.project.service<DbtProjectService>()
        val isInsideFunctionCall = parameters.position.parent is DjangoVariableReferenceImpl &&
                parameters.position.parent.parent is Jinja2FunctionCall &&
                (parameters.position.parent.parent as Jinja2FunctionCall
                        ).callee is DjangoVariableReferenceImpl
        val isAfterPartialFunctionCall = parameters.position.parent.nextSibling is PsiErrorElement &&
                ((parameters.position.parent.nextSibling as PsiErrorElement).errorDescription
                        == "Closing parenthesis expected")
        val isFunctionCall = isInsideFunctionCall || isAfterPartialFunctionCall

        val outerFunctionCallRef = when {
            isInsideFunctionCall ->
                ((parameters.position.parent.parent as Jinja2FunctionCall).callee
                        as DjangoVariableReferenceImpl)
            isAfterPartialFunctionCall ->
                (parameters.position.parent.prevSibling.prevSibling) as DjangoVariableReferenceImpl
            else -> null
        }

        if (isFunctionCall && outerFunctionCallRef?.name == "ref") {
            resultSet.addAllElements(projectService
                    .findDbtProjectModule(outerFunctionCallRef.containingFile)
                    ?.findAllModels()
                    ?.map { DbtModelCompletionLookup(it, addQuotes = true) }
                    ?: arrayListOf())
            resultSet.stopHere()
        }

        if (isFunctionCall && outerFunctionCallRef?.name == "source") {
            val functionCall = outerFunctionCallRef.parent as Jinja2FunctionCall
            val selectedParameterIndex = DbtJinja2FunctionParameterUtil.selectedParameterIndex(
                    parameters.position, functionCall)
            if (isInsideFunctionCall && selectedParameterIndex == 1) {
                val parameter = DbtJinja2FunctionParameterUtil.getParameter(functionCall, 0)
                if (parameter is DjangoStringLiteralImpl) {
                    resultSet.addAllElements(projectService
                            .findDbtProjectModule(outerFunctionCallRef.containingFile)
                            ?.findAllSourceTables(parameter.stringValue)
                            ?.map { DbtSourceTableCompletionLookup(it, addQuotes = true) }
                            ?: arrayListOf())
                    resultSet.stopHere()
                }
            } else {
                resultSet.addAllElements(projectService
                        .findDbtProjectModule(outerFunctionCallRef.containingFile)
                        ?.findAllSourceSchemas()
                        ?.map { DbtSourceSchemaCompletionLookup(it, addQuotes = true, addTrailingComma = true, autoPopup = true) }
                        ?: arrayListOf())
                resultSet.stopHere()
            }
        }
    }
}
