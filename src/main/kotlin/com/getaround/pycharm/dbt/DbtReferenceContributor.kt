package com.getaround.pycharm.dbt

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.django.lang.template.psi.impl.DjangoStringLiteralImpl
import com.jetbrains.django.lang.template.psi.impl.DjangoVariableReferenceImpl
import com.jetbrains.jinja2.tags.Jinja2FunctionCall


class DbtReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(DjangoStringLiteralImpl::class.java),
                object : PsiReferenceProvider() {
                    override fun getReferencesByElement(element: PsiElement,
                                                        context: ProcessingContext): Array<PsiReference> {
                        if (element.parent !is Jinja2FunctionCall) return PsiReference.EMPTY_ARRAY

                        val functionCall: Jinja2FunctionCall = element.parent as Jinja2FunctionCall
                        val callee: DjangoVariableReferenceImpl = functionCall.callee as DjangoVariableReferenceImpl

                        if (callee.name == "ref") {
                            val refVal: DjangoStringLiteralImpl? = PsiTreeUtil.findChildOfType<DjangoStringLiteralImpl>(functionCall, DjangoStringLiteralImpl::class.java)
                            if (refVal != null)
                                return arrayOf(DbtRefReference(refVal, refVal.stringValueTextRange))
                        }

                        if (callee.name == "source") {
                            val args = PsiTreeUtil.findChildrenOfType<DjangoStringLiteralImpl>(functionCall, DjangoStringLiteralImpl::class.java).toList()
                            if (args.size == 2) {
                                val sourceName = args[0]
                                val tableName = args[1]

                                return if (element == sourceName) {
                                    arrayOf(DbtSourceSchemaReference(
                                            element, (element as DjangoStringLiteralImpl).stringValueTextRange))
                                } else {
                                    arrayOf(DbtSourceTableReference(
                                            element, (element as DjangoStringLiteralImpl).stringValueTextRange,
                                            sourceName, sourceName.stringValueTextRange,
                                            tableName, tableName.stringValueTextRange))
                                }
                            } else if (args.size == 1) {
                                return arrayOf(DbtSourceSchemaReference(
                                        element, (element as DjangoStringLiteralImpl).stringValueTextRange))
                            }
                        }

                        return PsiReference.EMPTY_ARRAY
                    }
                })

    }
}