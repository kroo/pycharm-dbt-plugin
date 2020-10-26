package com.getaround.pycharm.dbt.docs

import com.getaround.pycharm.dbt.DbtJinja2BuiltinFunction
import com.getaround.pycharm.dbt.DbtJinja2Function
import com.getaround.pycharm.dbt.services.DbtContextValue
import com.getaround.pycharm.dbt.services.resolveResultType
import com.getaround.pycharm.dbt.services.resolveValueType
import com.github.rjeschke.txtmark.Configuration
import com.github.rjeschke.txtmark.Processor
import com.intellij.codeInsight.documentation.DocumentationManagerProtocol
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.python.documentation.PyDocumentationLink
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.types.TypeEvalContext
import org.markdown4j.CodeBlockEmitter

fun DbtJinja2Function.renderMarkdownDocContent(): String? {
    val conf = Configuration.builder()
            .forceExtentedProfile()
            .setCodeBlockEmitter(CodeBlockEmitter())
            .build()
    return Processor.process(doc, conf)
}

fun DbtContextValue.renderMarkdownDocContent(): String? {
    val conf = Configuration.builder()
            .forceExtentedProfile()
            .setCodeBlockEmitter(CodeBlockEmitter())
            .build()
    return Processor.process(doc, conf)
}

fun DbtJinja2Function.renderLinkedDefinitionDocContent(element: PsiElement): String {
    val typeDesc = if (this is DbtJinja2BuiltinFunction) "builtin function" else "macro"
    //language=HTML
    val type = "<span style='font-style: italic'>$typeDesc</span>"
    //language=HTML
    return """$type <b>$name</b>(${args.first().joinToString(", ")})"""
}

fun DbtJinja2BuiltinFunction.renderLinkedDefinitionDocContent(element: PsiElement): String {
    val project = element.project
    val args = ctxMethod.args.joinToString(", ") {
        "${it.name}: ${pylinkDoc(project, it.value, it.resolveValueType(project))}"
    }
    var resultLink = pylinkDoc(project, ctxMethod.result, ctxMethod.resolveResultType(project)).orEmpty()
    if (resultLink != "") resultLink = " -> $resultLink"

    //language=HTML
    return """<span style='font-style: italic'>builtin function</span> <b>$name</b>($args)$resultLink"""
}

fun DbtContextValue.renderLinkedDefinitionDocContent(element: PsiElement): String {
    val project = element.project
    val typeDesc = "builtin object"
    //language=HTML
    var valueLink = pylinkDoc(project, value, this.resolveValueType(project)).orEmpty()
    if (valueLink != "") valueLink = ": $valueLink"
    val type = "<span style='font-style: italic'>$typeDesc</span>"
    //language=HTML
    return """$type <b>$name</b>$valueLink"""
}


private fun pylinkDoc(project: Project, text: String, type: PyElement?): String? {
    val typeEvalContext = TypeEvalContext.codeInsightFallback(project)
    return type?.let { PyDocumentationLink.toPossibleClass(text, it, typeEvalContext) }
}

fun DbtJinja2Function.renderMarkdownDoc(element: PsiElement): String? {
    var renderDefinitionDocContent = renderLinkedDefinitionDocContent(element)
    if (this is DbtJinja2BuiltinFunction) {
        renderDefinitionDocContent = this.renderLinkedDefinitionDocContent(element)
    }

    val renderMarkdownDocContent = renderMarkdownDocContent()

    val wrappedMarkdownDocContent = if (renderMarkdownDocContent != null) {
        DocumentationMarkup.CONTENT_START + renderMarkdownDocContent + DocumentationMarkup.CONTENT_END
    } else ""

    return DocumentationMarkup.DEFINITION_START + renderDefinitionDocContent + DocumentationMarkup.DEFINITION_END +
            wrappedMarkdownDocContent
}

fun DbtContextValue.renderMarkdownDoc(element: PsiElement): String? {

    var renderDefinitionDocContent = renderLinkedDefinitionDocContent(element)
    val renderMarkdownDocContent = renderMarkdownDocContent()

    val wrappedMarkdownDocContent = if (renderMarkdownDocContent != null) {
        DocumentationMarkup.CONTENT_START + renderMarkdownDocContent + DocumentationMarkup.CONTENT_END
    } else ""

    return DocumentationMarkup.DEFINITION_START + renderDefinitionDocContent + DocumentationMarkup.DEFINITION_END +
            wrappedMarkdownDocContent
}