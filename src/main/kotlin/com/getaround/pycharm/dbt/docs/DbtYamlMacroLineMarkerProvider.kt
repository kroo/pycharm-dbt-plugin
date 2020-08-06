package com.getaround.pycharm.dbt.docs

import com.getaround.pycharm.dbt.services.DbtProjectService
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PlatformIcons
import org.jetbrains.yaml.psi.YAMLKeyValue

class DbtYamlMacroLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element.parent?.parent !is YAMLKeyValue || PsiTreeUtil.getParentOfType(element.parent?.parent, YAMLKeyValue::class.java)?.keyText != "macros") return
        val macroTag = element.parent?.parent as YAMLKeyValue
        if (macroTag.keyText != "name") return
        val macroTagName = macroTag.valueText
        val service = element.project.service<DbtProjectService>()
        val module = service.findDbtProjectModule(element.containingFile)
        val macro = module?.findMacro(macroTagName) ?: return
        val resultItem = NavigationGutterIconBuilder.create(PlatformIcons.METHOD_ICON)
                .setTarget(macro.element.nameElement)
                .setTooltipText("Macro definition")

        result.add(resultItem.createLineMarkerInfo(element))
    }
}
