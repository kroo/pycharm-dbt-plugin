package com.getaround.pycharm.dbt.completion

import com.intellij.psi.PsiElement
import com.jetbrains.jinja2.tags.Jinja2FunctionCall

object DbtJinja2FunctionParameterUtil {
    /**
     * Index of the parameter within functionCall that elem falls into
     *
     * This is 0 indexed -- 0 is the first parameter, 1 is the second, etc.
     */
    fun selectedParameterIndex(elem: PsiElement?, functionCall: Jinja2FunctionCall): Int? {
        var elementAtContext = elem
        while (elementAtContext != null && elementAtContext.parent != null && elementAtContext.parent !is Jinja2FunctionCall)
            elementAtContext = elementAtContext.parent
        if (elementAtContext == null) return null

        var currentParam = 0
        for (child in functionCall.children) {
            if (child == elementAtContext) {
                break
            }
            if (child != functionCall.callee)
                currentParam++
        }

        return currentParam
    }

    /**
     * Total number of parameters currently specified in the function call
     */
    fun countParameters(functionCall: Jinja2FunctionCall): Int? {
        return functionCall.children.filter { it != functionCall.callee }.size
    }

    /**
     * Get the element representing the parameter designed with the specified index
     */
    fun getParameter(functionCall: Jinja2FunctionCall, index: Int): PsiElement {
        return functionCall.children.filter { it != functionCall.callee }[index]
    }
}