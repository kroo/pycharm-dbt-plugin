package com.getaround.pycharm.dbt.docs

import com.getaround.pycharm.dbt.services.DbtProjectService
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.util.PlatformIcons.PROPERTIES_ICON
import com.jetbrains.django.lang.template.psi.impl.DjangoNamedExpressionImpl
import com.jetbrains.jinja2.tags.Jinja2MacroTag

class DbtJinja2MacroLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element.parent?.parent is Jinja2MacroTag && element.parent is DjangoNamedExpressionImpl) {
            val macroTag = element.parent?.parent as Jinja2MacroTag
            val macroTagName = macroTag.name ?: return
            val service = element.project.service<DbtProjectService>()
            val module = service.findDbtProjectModule(element.containingFile)
            val macroProperties = module?.findMacroProperties(macroTagName) ?: return
            val resultItem = NavigationGutterIconBuilder.create(PROPERTIES_ICON)
                    .setTarget(macroProperties)
                    .setTooltipText("Macro properties")
            result.add(resultItem.createLineMarkerInfo(element))
        }
    }
}
