package com.getaround.pycharm.dbt.completion

import com.getaround.pycharm.dbt.DbtJinja2Function
import com.getaround.pycharm.dbt.DbtJinja2Macro
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement

class DbtJinja2FunctionCompletionLookup(
    private val function: DbtJinja2Function,
    private val appendParens: Boolean,
    private val autoPopup: Boolean,
    private val appendInnerQuotes: Boolean = false
) : LookupElement() {
    override fun getLookupString(): String {
        return function.name
    }

    override fun getPsiElement(): PsiElement? {
        if (function is DbtJinja2Macro) {
            return function.element
        }
        return null
    }

    override fun renderElement(presentation: LookupElementPresentation?) {
        presentation?.itemText = lookupString
        presentation?.icon = AllIcons.Nodes.Method
        presentation?.appendTailText("(...)", true)
    }

    override fun handleInsert(context: InsertionContext) {
        super.handleInsert(context)
        if (appendParens) {
            val ind = context.tailOffset
            if (appendInnerQuotes) {
                context.document.insertString(ind, "(\"\")")
                context.editor.caretModel.moveToOffset(ind + 2)
            } else {
                context.document.insertString(ind, "()")
                context.editor.caretModel.moveToOffset(ind + 1)
            }
        }

        if (autoPopup) {
            AutoPopupController.getInstance(context.project).autoPopupMemberLookup(context.editor, null)
        }
    }
}
