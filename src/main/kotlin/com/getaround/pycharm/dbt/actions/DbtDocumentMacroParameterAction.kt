package com.getaround.pycharm.dbt.actions

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.jinja2.tags.Jinja2MacroParameter
import com.jetbrains.jinja2.tags.Jinja2MacroTag
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLSequenceItem
import org.jetbrains.yaml.psi.YAMLValue
import org.jetbrains.yaml.psi.impl.YAMLMappingImpl

class DbtDocumentMacroParameterAction : DbtDocumentMacroAction() {
    companion object {
        val ARGUMENTS_KEY = "arguments"
    }

    override fun getFamilyName(): String {
        return "Add description for macro parameter"
    }

    override fun getText(): String {
        return "Add description for macro parameter"
    }

    override fun updateMacroProperties(project: Project, macroProperties: YAMLSequenceItem?, element: PsiElement) {
        val parameterName = findParameterName(element)
        val yamlElementGenerator = YAMLElementGenerator.getInstance(project)
        WriteCommandAction.runWriteCommandAction(project, text, null, Runnable {
            val map = macroProperties.getMapping()
            var argumentsKeyVal = map?.getKeyValueByKey(ARGUMENTS_KEY)
            var addArgumentsToMap = false
            if (argumentsKeyVal == null) {
                argumentsKeyVal = yamlElementGenerator.createYamlKeyValue(ARGUMENTS_KEY, "")
                addArgumentsToMap = true
            }

            var argumentsSequence: YAMLSequence? = argumentsKeyVal.value.toSequence()
            var addSeqToArguments = false
            if (argumentsSequence !is YAMLSequence) {
                val dummyYaml = yamlElementGenerator.createDummyYamlWithText("- name: $parameterName")
                val seq = PsiTreeUtil.collectElementsOfType(dummyYaml, YAMLSequence::class.java).first()
                addSeqToArguments = true
                argumentsSequence = seq
            }

            var paramSeqItem = argumentsSequence?.items?.firstOrNull {
                it?.getMapping()?.getKeyValueByKey("name")?.valueText == parameterName
            }
            var addSeqItemToSeq = false
            if (paramSeqItem !is YAMLSequenceItem) {
                val dummyYaml = yamlElementGenerator.createDummyYamlWithText("- name: $parameterName\n  description:")
                val seqItem = PsiTreeUtil.collectElementsOfType(dummyYaml, YAMLSequenceItem::class.java).first()
                addSeqItemToSeq = true
                paramSeqItem = seqItem
            }

            val paramSeqItemMap = paramSeqItem.getMapping()
            val yamlKeyValue = yamlElementGenerator.createYamlKeyValue(
                    "description", " "
            )
            paramSeqItemMap?.add(yamlElementGenerator.createEol())
            paramSeqItemMap?.add(yamlKeyValue)

            if (addSeqItemToSeq && paramSeqItem != null) {
                argumentsSequence?.add(yamlElementGenerator.createEol())
                argumentsSequence?.add(paramSeqItem)
            }

            if (addSeqToArguments && argumentsSequence != null) {
                argumentsKeyVal.add(yamlElementGenerator.createEol())
                argumentsKeyVal.add(argumentsSequence)
            }

            if (addArgumentsToMap) {
                map?.add(yamlElementGenerator.createEol())
                map?.add(argumentsKeyVal)
            }
        })
    }

    override fun navigateToMacroProperties(project: Project, macroProperties: YAMLSequenceItem, element: PsiElement) {
        val descriptionOffset = findParameterByName(macroProperties, findParameterName(element))
                ?.textRange
                ?.endOffset
                ?: 0

        val vf = macroProperties.containingFile?.virtualFile ?: return
        OpenFileDescriptor(project, vf, descriptionOffset).navigate(true)
    }

    override fun jinja2MacroTag(element: PsiElement): Jinja2MacroTag? {
        if (element.parent?.parent?.parent is Jinja2MacroTag &&
                element.parent?.parent is Jinja2MacroParameter) {
            return element.parent.parent.parent as Jinja2MacroTag
        }
        return null
    }

    override fun isAvailable(macroProperties: YAMLSequenceItem?, element: PsiElement): Boolean {
        if (macroProperties == null) return true
        val parameterByName = findParameterByName(macroProperties, findParameterName(element))
        return parameterByName?.getKeyValueByKey("description") == null
    }

    private fun findParameterName(element: PsiElement): String? {
        return findParameter(element).nameElement?.text
    }

    private fun findParameter(element: PsiElement): Jinja2MacroParameter {
        return element.parent.parent as Jinja2MacroParameter
    }

    private fun findParameterByName(macroProperties: YAMLSequenceItem?, parameterName: String?): YAMLMapping? {
        if (parameterName == null) return null
        val map = macroProperties.getMapping()
        val arguments = map?.getKeyValueByKey(ARGUMENTS_KEY)?.value.toSequence()
        val argumentItems = arguments?.items.orEmpty()

        return argumentItems.map { it.getMapping() }.firstOrNull {
            it?.getKeyValueByKey("name")?.valueText == parameterName
        }
    }
}

fun YAMLSequenceItem?.getMapping(): YAMLMappingImpl? {
    if (this?.children?.get(0) !is YAMLMappingImpl) return null
    return this.children[0] as YAMLMappingImpl?
}

fun YAMLValue?.toSequence(): YAMLSequence? {
    if (this !is YAMLSequence) return null
    return this
}
