package com.getaround.pycharm.dbt.annotators

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.django.lang.template.psi.impl.DjangoStringLiteralImpl
import com.jetbrains.django.lang.template.psi.impl.DjangoVariableReferenceImpl
import com.jetbrains.jinja2.tags.Jinja2FunctionCall

class RefAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is Jinja2FunctionCall) return

        val functionCall: Jinja2FunctionCall = element
        val callee: DjangoVariableReferenceImpl = functionCall.callee as DjangoVariableReferenceImpl
        val refVal: DjangoStringLiteralImpl? = PsiTreeUtil.findChildOfType<DjangoStringLiteralImpl>(
                functionCall, DjangoStringLiteralImpl::class.java)

        if (callee.name == "ref" && refVal != null) {
            holder
                    .newAnnotation(HighlightSeverity.INFORMATION, "")
                    .range(refVal)
                    .create()
        }
    }
}
