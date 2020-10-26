package com.getaround.pycharm.dbt.completion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.django.completion.DjangoItemCompletionLookup
import icons.DatabaseIcons
import javax.swing.Icon

open class DbtCompletionLookup(
        private val element: PsiElement,
        private val addQuotes: Boolean = false,
        private val icon: Icon? = null,
        private val autoPopup: Boolean = false
) : DjangoItemCompletionLookup(element) {
    override fun getLookupString(): String {
        val name = element.text
        return if (!addQuotes) name else "\"$name\""
    }

    override fun isWorthShowingInAutoPopup(): Boolean {
        return true
    }

    override fun renderElement(presentation: LookupElementPresentation?) {
        super.renderElement(presentation)
        if (icon != null) {
            presentation?.icon = icon
        }
    }

    override fun handleInsert(context: InsertionContext) {
        super.handleInsert(context)

        if (autoPopup) {
            AutoPopupController.getInstance(context.project).autoPopupMemberLookup(context.editor, null)
        }
    }
}

class DbtModelCompletionLookup(
        private val element: PsiFile,
        private val addQuotes: Boolean = false
) : DbtCompletionLookup(element, addQuotes, icon = DatabaseIcons.Table) {
    override fun getLookupString(): String {
        val name = element.virtualFile.nameWithoutExtension
        return if (!addQuotes) name else "\"$name\""
    }
}

class DbtSourceSchemaCompletionLookup(
        private val element: PsiElement,
        private val addQuotes: Boolean = false,
        private val addTrailingComma: Boolean,
        autoPopup: Boolean = true
) : DbtCompletionLookup(element, addQuotes, icon = DatabaseIcons.Schema, autoPopup = autoPopup) {
    override fun getLookupString(): String {
        val name = element.text
        val trailingComma = if (!addTrailingComma) "" else ", "
        val result = if (!addQuotes) name else "\"$name\""
        return result + trailingComma
    }
}

class DbtSourceTableCompletionLookup(
        element: PsiElement,
        addQuotes: Boolean = false,
        autoPopup: Boolean = true
) : DbtCompletionLookup(element, addQuotes, icon = DatabaseIcons.Foreign_table, autoPopup = autoPopup)
