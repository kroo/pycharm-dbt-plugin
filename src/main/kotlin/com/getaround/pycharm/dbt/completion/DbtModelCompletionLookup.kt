package com.getaround.pycharm.dbt.completion

import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.django.completion.DjangoItemCompletionLookup
import icons.DatabaseIcons
import javax.swing.Icon

open class DbtCompletionLookup(
    private val element: PsiElement,
    private val addQuotes: Boolean = false,
    private val icon: Icon? = null
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
    element: PsiElement,
    addQuotes: Boolean = false
) : DbtCompletionLookup(element, addQuotes, icon = DatabaseIcons.External_schema_object)

class DbtSourceTableCompletionLookup(
    element: PsiElement,
    addQuotes: Boolean = false
) : DbtCompletionLookup(element, addQuotes, icon = DatabaseIcons.Foreign_table)
