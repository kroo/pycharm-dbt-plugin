package com.getaround.pycharm.dbt.completion

import com.getaround.pycharm.dbt.services.DbtContextValue
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.FakePsiElement
import com.intellij.util.PlatformIcons
import javax.swing.Icon


class DbtJinja2ValueCompletionLookup(
        private val origElement: PsiElement,
        private val value: DbtContextValue,
        private val appendDot: Boolean,
        private val autoPopup: Boolean
) : LookupElement() {
    override fun getLookupString(): String = value.name

    override fun getPsiElement(): PsiElement? {
        return DbtContextValueFakePsiElementFactory.create(
                resolveToPythonSource(value.name) ?: origElement, value)
    }

    private fun resolveToPythonSource(name: String): PsiElement? {
        return null
    }

    override fun renderElement(presentation: LookupElementPresentation?) {
        presentation?.itemText = lookupString
        presentation?.icon = AllIcons.Nodes.Constant
    }

    override fun handleInsert(context: InsertionContext) {
        super.handleInsert(context)
        if (appendDot) {
            val ind = context.tailOffset
                context.document.insertString(ind, ".")
                context.editor.caretModel.moveToOffset(ind + 1)
        }

        if (autoPopup) {
            AutoPopupController.getInstance(context.project).autoPopupMemberLookup(context.editor, null)
        }
    }
}


class DbtContextValueFakePsiElement internal constructor(
        element: PsiElement,
        val value: DbtContextValue
) :
        FakePsiElement() {
    private val myElement = element

    override fun isPhysical(): Boolean = false
    override fun getParent(): PsiElement = myElement
    override fun getContainingFile(): PsiFile? = null
    override fun canNavigate(): Boolean = false
    override fun canNavigateToSource(): Boolean = false
    override fun getIcon(open: Boolean): Icon? = PlatformIcons.FUNCTION_ICON
    override fun getName(): String? {
        return value.name
    }

    override fun getNavigationElement(): PsiElement {
        return myElement
    }
}


object DbtContextValueFakePsiElementFactory {
    private val elements = mutableMapOf<String, DbtContextValueFakePsiElement>()
    fun create(element: PsiElement, value: DbtContextValue): DbtContextValueFakePsiElement? {
        if (elements.containsKey(value.name)) {
            return elements[value.name]
        } else {
            val newElement = DbtContextValueFakePsiElement(element, value)
            elements[value.name] = newElement
            return newElement
        }
    }
}