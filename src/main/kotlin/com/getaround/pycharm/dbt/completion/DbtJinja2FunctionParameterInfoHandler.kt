package com.getaround.pycharm.dbt.completion

import com.getaround.pycharm.dbt.DbtJinja2Function
import com.getaround.pycharm.dbt.completion.DbtJinja2FunctionParameterUtil.countParameters
import com.getaround.pycharm.dbt.completion.DbtJinja2FunctionParameterUtil.selectedParameterIndex
import com.getaround.pycharm.dbt.services.DbtProjectService
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.parameterInfo.CreateParameterInfoContext
import com.intellij.lang.parameterInfo.ParameterInfoContext
import com.intellij.lang.parameterInfo.ParameterInfoHandler
import com.intellij.lang.parameterInfo.ParameterInfoUIContext
import com.intellij.lang.parameterInfo.ParameterInfoUIContextEx
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext
import com.intellij.openapi.components.service
import com.intellij.psi.PsiFile
import com.jetbrains.jinja2.tags.Jinja2FunctionCall
import com.jetbrains.rd.util.enumSetOf
import java.util.EnumSet

class DbtJinja2FunctionParameterInfoHandler : ParameterInfoHandler<Jinja2FunctionCall, DbtJinja2Function> {
    override fun showParameterInfo(element: Jinja2FunctionCall, context: CreateParameterInfoContext) {
        context.showHint(element, element.textOffset, this)
    }

    override fun updateParameterInfo(parameterOwner: Jinja2FunctionCall, context: UpdateParameterInfoContext) {
        val allegedCursorOffset = context.offset
        if (!parameterOwner.textRange.contains(allegedCursorOffset) && parameterOwner.text.endsWith(")")) {
            context.removeHint()
        } else {
            val offset = findCurrentParameter(parameterOwner, context.file, allegedCursorOffset)
            if (offset == null) {
                context.removeHint()
            } else {
                context.setCurrentParameter(offset)
            }
        }
    }

    override fun updateUI(func: DbtJinja2Function?, context: ParameterInfoUIContext) {
        context.isUIComponentEnabled = true

        val argCount = countParameters(context.parameterOwner as Jinja2FunctionCall)
        val params = func?.args?.filter { it.size == argCount }?.get(0)
        var hints: Array<String> = (params?.toTypedArray()) ?: arrayOf()
        if (hints.isEmpty()) {
            hints = arrayOf(CodeInsightBundle.message("parameter.info.no.parameters"))
        }
        if (context is ParameterInfoUIContextEx) {
            var flags: Array<EnumSet<ParameterInfoUIContextEx.Flag>> = hints.map {
                enumSetOf<ParameterInfoUIContextEx.Flag>()
            }.toTypedArray()
            if (params == null) {
                flags = arrayOf(EnumSet.of(ParameterInfoUIContextEx.Flag.DISABLE))
            } else {
                if (0 <= context.currentParameterIndex && context.currentParameterIndex < hints.size) {
                    flags[context.currentParameterIndex] = EnumSet.of(ParameterInfoUIContextEx.Flag.HIGHLIGHT)
                }
            }
            hints = hints
                    .zip(MutableList(hints.size) { i -> if (i == hints.lastIndex) "" else ", " })
                    .map { it.toList() }.flatten().toTypedArray()
            flags = flags
                    .zip(MutableList(flags.size) { enumSetOf<ParameterInfoUIContextEx.Flag>() })
                    .map { it.toList() }.flatten().toTypedArray()
            context.setupUIComponentPresentation(hints, flags, context.defaultParameterColor)
        } else {
            val text = hints.joinToString(", ")
            val highlightStartOffset = 0
            val highlightEndOffset = 0
            val isDisabled = params == null
            context.setupUIComponentPresentation(
                    text, highlightStartOffset, highlightEndOffset, isDisabled, false,
                    false, context.defaultParameterColor)
        }
    }

    override fun getParametersForLookup(item: LookupElement?, context: ParameterInfoContext?): Array<Any?>? {
        return arrayOf()
    }

    override fun couldShowInLookup(): Boolean {
        return true
    }

    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): Jinja2FunctionCall? {
        return findJinja2FunctionCall(context.file, context.offset)
    }

    override fun findElementForParameterInfo(context: CreateParameterInfoContext): Jinja2FunctionCall? {
        val call = findJinja2FunctionCall(context.file, context.offset)
        val callee = call?.callee ?: return null
        val dbtProjectModule = context.project.service<DbtProjectService>().findDbtProjectModule(context.file)
        val functions = dbtProjectModule?.findAllDbtFunctions()
        context.itemsToShow = functions.orEmpty().filter { it.name == callee.text }.toTypedArray()
        return call
    }

    private fun findJinja2FunctionCall(file: PsiFile?, offset: Int): Jinja2FunctionCall? {
        var elementAtContext = file?.findElementAt(offset)
        while (elementAtContext !is Jinja2FunctionCall && elementAtContext != null)
            elementAtContext = elementAtContext.parent
        if (elementAtContext == null) return null

        return elementAtContext as Jinja2FunctionCall
    }

    private fun findCurrentParameter(parameterOwner: Jinja2FunctionCall, file: PsiFile, offset: Int): Int? {
        val elementAtContext = file.findElementAt(offset)
        return selectedParameterIndex(elementAtContext, parameterOwner)
    }
}
