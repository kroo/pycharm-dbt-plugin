package com.getaround.pycharm.dbt.references

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.django.lang.template.psi.impl.DjangoStringLiteralImpl
import com.jetbrains.django.lang.template.psi.impl.DjangoVariableReferenceImpl
import com.jetbrains.jinja2.tags.Jinja2FunctionCall

class DbtReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(DjangoStringLiteralImpl::class.java),
                object : PsiReferenceProvider() {
                    override fun getReferencesByElement(
                        element: PsiElement,
                        context: ProcessingContext
                    ): Array<PsiReference> {
                        if (element.parent !is Jinja2FunctionCall) return PsiReference.EMPTY_ARRAY

                        val functionCall: Jinja2FunctionCall = element.parent as Jinja2FunctionCall
                        val callee: DjangoVariableReferenceImpl = functionCall.callee as DjangoVariableReferenceImpl

                        return when (callee.name) {
                            "ref" -> refReferences(functionCall)
                            "source" -> sourceReferences(functionCall, element)
                            else -> PsiReference.EMPTY_ARRAY
                        }
                    }
                })
    }

    private fun sourceReferences(functionCall: Jinja2FunctionCall, element: PsiElement): Array<PsiReference> {
        val args =
                PsiTreeUtil.findChildrenOfType<DjangoStringLiteralImpl>(
                        functionCall, DjangoStringLiteralImpl::class.java).toList()
        return if (args.size == 2 && element == args[0]) {
            arrayOf(DbtSourceSchemaReference(
                    element, (element as DjangoStringLiteralImpl).stringValueTextRange))
        } else if (args.size == 2) {
            arrayOf<PsiReference>(DbtSourceTableReference(
                    element, (element as DjangoStringLiteralImpl).stringValueTextRange,
                    args[0], args[0].stringValueTextRange,
                    args[1], args[1].stringValueTextRange))
        } else if (args.size == 1) {
            arrayOf<PsiReference>(DbtSourceSchemaReference(
                    element, (element as DjangoStringLiteralImpl).stringValueTextRange))
        } else {
            PsiReference.EMPTY_ARRAY
        }
    }

    private fun refReferences(functionCall: Jinja2FunctionCall): Array<PsiReference> {
        val refVal: DjangoStringLiteralImpl? =
                PsiTreeUtil.findChildOfType<DjangoStringLiteralImpl>(
                        functionCall, DjangoStringLiteralImpl::class.java)
        return if (refVal != null) {
            arrayOf(DbtRefReference(refVal, refVal.stringValueTextRange))
        } else {
            PsiReference.EMPTY_ARRAY
        }
    }
}
