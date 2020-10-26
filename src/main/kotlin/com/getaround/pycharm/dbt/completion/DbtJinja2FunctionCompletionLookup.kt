package com.getaround.pycharm.dbt.completion

import com.getaround.pycharm.dbt.DbtJinja2BuiltinFunction
import com.getaround.pycharm.dbt.DbtJinja2Function
import com.getaround.pycharm.dbt.DbtJinja2Macro
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.util.QualifiedName
import com.intellij.util.PlatformIcons
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyPsiFacade
import javax.swing.Icon


class DbtJinja2FunctionCompletionLookup(
        private val origElement: PsiElement,
        private val function: DbtJinja2Function,
        private val appendParens: Boolean,
        private val autoPopup: Boolean,
        private val appendInnerQuotes: Boolean = false
) : LookupElement() {
    override fun getLookupString(): String = function.name

    override fun getPsiElement(): PsiElement? {
        return when (function) {
            is DbtJinja2Macro -> function.element
            is DbtJinja2BuiltinFunction ->
                DbtJinja2BuiltinFunctionFakePsiElementFactory.create(
                        resolveToPythonSource(function.name) ?: origElement, function)
            else -> null
        }
    }

    private fun resolveToPythonSource(name: String): PsiElement? {
        val contexts = listOf(
                Pair("dbt.context.base", "BaseContext"),
                Pair("dbt.context.target", "TargetContext"),
                Pair("dbt.context.providers", "ModelContext"),
                Pair("dbt.context.providers", "MacroContext")
        )
        for (context in contexts) {
            val baseQn = QualifiedName.fromDottedString(context.first)
            val psiFacade = PyPsiFacade.getInstance(origElement.project)
            val resolveContext = psiFacade.createResolveContextFromFoothold(origElement.parent)
            val fileElems = psiFacade.resolveQualifiedName(baseQn, resolveContext)
            val fileElem = fileElems.firstOrNull()
            if (fileElem is PyFile) {
                val classElem = fileElem.topLevelClasses.firstOrNull { it.name == context.second }
                if (classElem is PyClass) {
                    val methodElem = classElem.methods.filter { it.name == name }.firstOrNull()
                    if (methodElem is PyFunction) {
                        return methodElem
                    }
                }
            }
        }
        return null
    }

    override fun renderElement(presentation: LookupElementPresentation?) {
        presentation?.itemText = lookupString
        presentation?.icon = when (function) {
            is DbtJinja2Macro -> AllIcons.Nodes.Method
            is DbtJinja2BuiltinFunction -> AllIcons.Nodes.Function
            else -> null
        }


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


class DbtJinja2BuiltinFunctionFakePsiElement internal constructor(
        element: PsiElement,
        val function: DbtJinja2BuiltinFunction
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
        return function.name
    }

    override fun getNavigationElement(): PsiElement {
        return myElement
    }
}


object DbtJinja2BuiltinFunctionFakePsiElementFactory {
    private val elements = mutableMapOf<String, DbtJinja2BuiltinFunctionFakePsiElement>()
    fun create(element: PsiElement, function: DbtJinja2BuiltinFunction): DbtJinja2BuiltinFunctionFakePsiElement? {
        if (elements.containsKey(function.name)) {
            return elements[function.name]
        } else {
            val newElement = DbtJinja2BuiltinFunctionFakePsiElement(element, function)
            elements[function.name] = newElement
            return newElement
        }
    }
}