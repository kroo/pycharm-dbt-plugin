package com.getaround.pycharm.dbt.module

import com.getaround.pycharm.dbt.DbtJinja2BuiltinFunction
import com.getaround.pycharm.dbt.DbtJinja2Function
import com.getaround.pycharm.dbt.DbtJinja2Macro
import com.getaround.pycharm.dbt.services.DbtTypeService
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.extensions.python.toPsi
import com.jetbrains.jinja2.tags.Jinja2MacroTag
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLSequenceItem
import org.yaml.snakeyaml.Yaml

class DbtModule(projectYmlFileOrig: PsiFile) {
    companion object {
        enum class ResourceType {
            MODELS, SOURCES, SEEDS, SNAPSHOTS, ANALYSES, MACROS
        }
    }

    private val projectYmlFileRef = SmartPointerManager
            .getInstance(projectYmlFileOrig.project)
            .createSmartPsiElementPointer(projectYmlFileOrig, projectYmlFileOrig)
    val projectYmlFile: PsiFile get() = projectYmlFileRef.element!!

    private val project = projectYmlFile.project
    val projectRoot: PsiDirectory get() = projectYmlFile.containingDirectory

    private val sourcePaths: Collection<VirtualFile>
        get() = getPaths("source-paths")

    private val dataPaths: Collection<VirtualFile>
        get() = getPaths("data-paths")

    private val snapshotPaths: Collection<VirtualFile>
        get() = getPaths("snapshot-paths")

    private val analysesPaths: Collection<VirtualFile>
        get() = getPaths("analysis-paths")

    private val macroPaths: Collection<VirtualFile>
        get() = getPaths("macro-paths")

    private val testPaths: Collection<VirtualFile>
        get() = getPaths("test-paths")

    private fun getPaths(key: String): List<VirtualFile> {
        val obj = this.asMap
        if (obj?.get(key) !is List<*>) return arrayListOf()
        val sourcePaths = obj[key] as List<*>

        return sourcePaths.mapNotNull { projectRoot.findSubdirectory(it as String)?.virtualFile }
    }

    /**
     * Returns the collection of all schema.yml files found in the project
     */
    private fun findAllResourcePropertyFiles(resourceType: ResourceType = ResourceType.MODELS):
            List<DbtResourceDescriptionFile> {
        PsiManager.getInstance(project)
        val allSchemaFiles = FilenameIndex.getAllFilesByExt(
                project, "yml", GlobalSearchScope.projectScope(project))
        return allSchemaFiles.asSequence()
                .filter { it.name != "dbt_project.yml" }
                .filter { yFl ->
                    val paths = sourcePaths.toMutableList()
                    if (resourceType == ResourceType.SEEDS) paths.addAll(dataPaths)
                    if (resourceType == ResourceType.SNAPSHOTS) paths.addAll(snapshotPaths)
                    if (resourceType == ResourceType.ANALYSES) paths.addAll(analysesPaths)
                    if (resourceType == ResourceType.MACROS) paths.addAll(macroPaths)
                    paths.any { sp ->
                        VfsUtilCore.isAncestor(sp, yFl, false)
                    }
                }
                .map { DbtResourceDescriptionFile(it.toPsi(project) as YAMLFile) }.toList()
    }

    /**
     * Parse the entire contents of the project yaml file.  Slow!
     */
    private val asMap get(): Map<String, Any>? = Yaml().load(projectYmlFile.text)

    /**
     * Find's a dbt model inside this dbt project module.
     *
     * @param name: the name of the model to find in this project, excluding the .sql extension!
     */
    fun findModel(name: String): PsiFile? {
        val results = findAllModels().filter { it.name == "$name.sql" }

        if (results.isEmpty()) return null
        return results[0]
    }

    /**
     * Find the list of all dbt model files in this project
     */
    fun findAllModels(): List<PsiFile> {
        return FilenameIndex
                .getAllFilesByExt(project, "sql")
                .filter { sqlFile ->
                    sourcePaths.any { sourcePath ->
                        VfsUtilCore.isAncestor(sourcePath, sqlFile, false)
                    }
                }.mapNotNull { PsiManager.getInstance(project).findFile(it) }
    }

    /**
     * Find a dbt source inside this dbt project module.
     *
     * @param sourceName: The first argument to source(), the name of the source schema
     * @param tableName: The second argument to source(), the name of the source table
     */
    fun findSourceTable(sourceName: String, tableName: String): PsiElement? {
        for (schemaFile in this.findAllResourcePropertyFiles()) {
            val sourceTable = schemaFile.getSourceTable(sourceName, tableName)
            if (sourceTable != null) {
                return sourceTable
            }
        }
        return null
    }

    /**
     * Returns a collection of all source schemas
     */
    fun findAllSourceTables(sourceName: String): Set<PsiElement> {
        val allTables = HashSet<PsiElement>()
        for (schemaFile in this.findAllResourcePropertyFiles(ResourceType.SOURCES)) {
            allTables.addAll(schemaFile.getAllSourceTables(sourceName))
        }
        return allTables
    }

    /**
     * Find a dbt source inside this dbt project module.
     *
     * @param sourceName: The first argument to source(), the name of the source schema
     */
    fun findSourceSchema(sourceName: String): PsiElement? {
        for (schemaFile in this.findAllResourcePropertyFiles(ResourceType.SOURCES)) {
            val sourceSchema = schemaFile.getSource(sourceName)
            if (sourceSchema != null) {
                val nameKey = sourceSchema.keysValues.filter { yamlKeyValue -> yamlKeyValue.keyText == "name" }
                return nameKey.first().value
            }
        }
        return null
    }

    /**
     * Returns a collection of all source schemas
     */
    fun findAllSourceSchemas(): Set<PsiElement> {
        val allSources = HashSet<PsiElement>()
        for (schemaFile in this.findAllResourcePropertyFiles(ResourceType.SOURCES)) {
            for (sourceSchema in schemaFile.getAllSources()) {
                val nameKey = sourceSchema.keysValues.filter { yamlKeyValue -> yamlKeyValue.keyText == "name" }
                val element = nameKey.first()?.value
                if (element != null) allSources.add(element)
            }
        }
        return allSources
    }

    /**
     * Find all sql files within 'macros' path
     */
    private fun findAllMacroFiles(): List<PsiFile> {
        return FilenameIndex
                .getAllFilesByExt(project, "sql")
                .filter { sqlFile ->
                    macroPaths.any { sourcePath ->
                        VfsUtilCore.isAncestor(sourcePath, sqlFile, false)
                    }
                }.mapNotNull { PsiManager.getInstance(project).findFile(it) }
    }

    /**
     * Find a macro definition
     */
    fun findMacro(name: String): DbtJinja2Macro? {
        for (macroFile in findAllMacroFiles()) {
            val macros = PsiTreeUtil.findChildrenOfType(macroFile, Jinja2MacroTag::class.java)
            for (macro in macros) {
                if (macro.nameElement?.name == name) {
                    return DbtJinja2Macro(macro)
                }
            }
        }
        return null
    }

    /**
     * Return a list of all dbt functions
     */
    fun findAllDbtFunctions(): List<DbtJinja2Function> {
        val result = mutableListOf<DbtJinja2Function>()
        for (macroFile in findAllMacroFiles()) {
            val macros = PsiTreeUtil.findChildrenOfType(macroFile, Jinja2MacroTag::class.java)
            for (macro in macros) {
                result.add(DbtJinja2Macro(macro))
            }
        }

        result.addAll(project.service<DbtTypeService>().builtinFunctions.map { DbtJinja2BuiltinFunction(it) })

        return result
    }

    /**
     * Find the resource property configuration for a macro
     */
    fun findMacroProperties(macroName: String): YAMLSequenceItem? {
        for (schemaFile in this.findAllResourcePropertyFiles(ResourceType.MACROS)) {
            val macroProperty = schemaFile.getMacro(macroName)

            if (macroProperty != null) {
                return macroProperty
            }
        }
        return null
    }

    /**
     * Returns true if the specified file is contained within any of the source-paths
     */
    fun isModelFile(containingFile: VirtualFile?): Boolean {
        if (containingFile?.extension != "sql") return false
        for (modelRoot in sourcePaths) {
            if (VfsUtil.isAncestor(modelRoot, containingFile, false)) {
                return true
            }
        }
        return false
    }

    fun isTestFile(containingFile: VirtualFile?): Boolean {
        if (containingFile?.extension != "sql") return false
        for (modelRoot in testPaths) {
            if (VfsUtil.isAncestor(modelRoot, containingFile, false)) {
                return true
            }
        }
        return false
    }
}
