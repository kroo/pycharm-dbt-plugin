package com.getaround.pycharm.dbt.actions

import com.getaround.pycharm.dbt.module.DbtModule
import com.getaround.pycharm.dbt.services.DbtProjectService
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.refactoring.BaseRefactoringIntentionAction
import com.jetbrains.jinja2.tags.Jinja2MacroTag
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLSequenceItem
import org.jetbrains.yaml.psi.impl.YAMLMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLSequenceImpl

open class DbtDocumentMacroAction : BaseRefactoringIntentionAction() {
    override fun getFamilyName(): String {
        return "Add description for macro"
    }

    override fun getText(): String = "Add description for macro"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (jinja2MacroTag(element) == null) return false
        val name = jinja2MacroTagName(element) ?: return false
        val module = dbtModule(element)
        val macroProperties = findMacroProperties(module, name)
        return isAvailable(macroProperties, element)
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val macroName = jinja2MacroTagName(element) ?: return
        val module = dbtModule(element)
        var macroProperties: YAMLSequenceItem? = findMacroProperties(module, macroName)
        val macroPropertiesFile: PsiFile? = ensurePropertiesFileExists(element, macroProperties)

        macroProperties = ensureMacroPropertiesContainsMacroName(project, module, macroName, macroPropertiesFile)

        updateMacroProperties(project, macroProperties, element)

        macroProperties = findMacroProperties(module, macroName)
        navigateToMacroProperties(project, macroProperties!!, element)
    }

    /**
     * Override me to modify macroProperties (see link #addDescriptionKey)
     */
    open fun updateMacroProperties(project: Project, macroProperties: YAMLSequenceItem?, element: PsiElement) {
        addDescriptionKey(project, macroProperties)
    }

    /**
     * Override me to change the navigation behavior after the refactor is complete
     */
    open fun navigateToMacroProperties(project: Project, macroProperties: YAMLSequenceItem, element: PsiElement) {
        val descriptionOffset = macroProperties
                .keysValues
                .firstOrNull { it.keyText == "description" }
                ?.textRange
                ?.endOffset
                ?: 0

        val vf = macroProperties.containingFile?.virtualFile ?: return
        OpenFileDescriptor(project, vf, descriptionOffset).navigate(true)
    }

    /**
     * Override me to set the path to the jinja2 macro tag
     */
    open fun jinja2MacroTag(element: PsiElement): Jinja2MacroTag? {
        if (element.parent?.parent is Jinja2MacroTag) {
            return element.parent.parent as Jinja2MacroTag
        }
        return null
    }

    /**
     * Override me to set the criteria for wether this macro action is available
     */
    open fun isAvailable(macroProperties: YAMLSequenceItem?, element: PsiElement) =
            macroProperties == null || macroProperties.keysValues.none { it.keyText == "description" }

    private fun ensurePropertiesFileExists(element: PsiElement, macroProperties: YAMLSequenceItem?): PsiFile {
        var macroPropertiesFile = macroProperties?.containingFile
        val propertiesFileName = element.containingFile.virtualFile.nameWithoutExtension

        val containingDirectory = element.containingFile.containingDirectory
        val fileName = "$propertiesFileName.yml"
        if (macroPropertiesFile == null) macroPropertiesFile = containingDirectory.findFile(fileName)

        if (macroPropertiesFile == null) {
            // create a file with the same name as the file containing the macro
            WriteCommandAction.runWriteCommandAction(element.project, text, null, Runnable {
                val fileFactory = PsiFileFactory.getInstance(element.project)
                macroPropertiesFile = fileFactory.createFileFromText(fileName, YAMLFileType.YML,
                        propertiesFileTemplate(jinja2MacroTagName(element).orEmpty()))
                containingDirectory.add(macroPropertiesFile as PsiFile)
            })
        }

        return macroPropertiesFile!!
    }

    private fun ensureMacroPropertiesContainsMacroName(
        project: Project,
        module: DbtModule?,
        macroName: String,
        macroPropertiesFile: PsiFile?
    ): YAMLSequenceItem? {

        val macroProperties = findMacroProperties(module, macroName)

        if (macroProperties == null && macroPropertiesFile != null) {
            WriteCommandAction.runWriteCommandAction(project, text, null, Runnable {
                val file = macroPropertiesFile as YAMLFile
                val dummyYaml = yamlElementGenerator(project).createDummyYamlWithText(propertiesFileTemplate(macroName))
                val dummyDocument = dummyYaml.documents.first()
                val dummyMapping = dummyDocument.topLevelValue as YAMLMappingImpl
                val dummyMacroKV = dummyMapping.getKeyValueByKey("macros")!!
                val dummyMacrosSequenceItem = (dummyMacroKV.value as YAMLSequence).items.first()
                if (file.documents.isEmpty()) {
                    file.add(dummyDocument)
                } else {
                    val document = file.documents.first()
                    val map = document.topLevelValue as YAMLMappingImpl
                    val macrosKV = map.getKeyValueByKey("macros")
                    if (macrosKV == null || macrosKV.value !is YAMLSequenceImpl) {
                        map.putKeyValue(dummyMacroKV)
                    } else {
                        (macrosKV.value as YAMLSequenceImpl).add(yamlElementGenerator(project).createEol())
                        (macrosKV.value as YAMLSequenceImpl).add(dummyMacrosSequenceItem)
                    }
                }
            }, macroPropertiesFile)
        }
        return findMacroProperties(module, macroName)
    }

    private fun addDescriptionKey(project: Project, macroProperties: YAMLSequenceItem?) {
        WriteCommandAction.runWriteCommandAction(project, text, null, Runnable {
            val yamlKeyValue = yamlElementGenerator(project).createYamlKeyValue(
                    "description", " "
            )
            macroProperties?.children?.get(0)?.add(yamlElementGenerator(project).createEol())
            macroProperties?.children?.get(0)?.add(yamlKeyValue)
        })
    }

    private fun jinja2MacroTagName(element: PsiElement) = jinja2MacroTag(element)?.name

    private fun dbtModule(element: PsiElement) =
            element.project.service<DbtProjectService>().findDbtProjectModule(element.containingFile)

    private fun findMacroProperties(module: DbtModule?, macroName: String) =
            module?.findMacroProperties(macroName)

    private fun propertiesFileTemplate(macroName: String) = "version: 2\n\nmacros:\n  - name: $macroName\n"

    private fun yamlElementGenerator(project: Project) = YAMLElementGenerator.getInstance(project)
}
