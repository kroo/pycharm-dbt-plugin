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
    override fun getLookupString(): String {
        return function.name
    }

    override fun getPsiElement(): PsiElement? {
        return if (function is DbtJinja2Macro) {
            function.element
        } else if (function is DbtJinja2BuiltinFunction) {
            DbtJinja2BuiltinFunctionFakePsiElement(
                    resolveToPythonSource(function.name) ?: origElement, function)
        } else {
            null
        }
    }

    fun resolveToPythonSource(name: String): PsiElement? {
        val baseQn = QualifiedName.fromDottedString("dbt.context.base")
        val psiFacade = PyPsiFacade.getInstance(origElement.project)
        val context = psiFacade.createResolveContextFromFoothold(origElement.parent)
        val fileElems = psiFacade.resolveQualifiedName(baseQn, context)
        val fileElem = fileElems.firstOrNull()
        if (fileElem is PyFile) {
            val classElem = fileElem.topLevelClasses.firstOrNull { it.name == "BaseContext" }
            if (classElem is PyClass) {
                val methodElem = classElem.methods.filter { it.name == name }.firstOrNull()
                if (methodElem is PyFunction) {
                    return methodElem
                }
            }
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

class DbtJinja2BuiltinFunctionFakePsiElement(
    element: PsiElement,
    val function: DbtJinja2BuiltinFunction
) :
        FakePsiElement() {
    private val myElement = element

    override fun isPhysical(): Boolean = false
    override fun getParent(): PsiElement = myElement
    override fun getContainingFile(): PsiFile? = null
    override fun canNavigate(): Boolean = true
    override fun canNavigateToSource(): Boolean = true
    override fun getIcon(open: Boolean): Icon? = PlatformIcons.FUNCTION_ICON
    override fun getName(): String? {
        return function.name
    }

    override fun getNavigationElement(): PsiElement {
        return myElement
    }
}
