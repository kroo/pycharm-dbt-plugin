package com.getaround.pycharm.dbt.module

import com.getaround.pycharm.dbt.DbtJinja2Function
import com.getaround.pycharm.dbt.DbtJinja2Functions
import com.getaround.pycharm.dbt.DbtJinja2Macro
import com.getaround.pycharm.dbt.DbtPsiUtil
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
import com.jetbrains.jinja2.tags.Jinja2MacroTag
import org.jetbrains.yaml.psi.impl.YAMLFileImpl
import org.yaml.snakeyaml.Yaml

class DbtModule(projectYmlFileOrig: PsiFile) {
    private val projectYmlFileRef = SmartPointerManager
            .getInstance(projectYmlFileOrig.project)
            .createSmartPsiElementPointer(projectYmlFileOrig, projectYmlFileOrig)
    val projectYmlFile: PsiFile get() = projectYmlFileRef.element!!

    override fun toString(): String {
        return "DbtModule(${containingDirectory.name})"
    }

    val containingDirectory: PsiDirectory get() = projectYmlFile.containingDirectory

    /**
     * Get the name of the project
     */
    val projectName: String?
        get() {
            val obj = this.asMap
            return obj?.get("name") as String?
        }

    private val sourcePaths: Collection<VirtualFile>
        get() = getPaths("source-paths")

    private val macroPaths: Collection<VirtualFile>
        get() = getPaths("macro-paths")

    private fun getPaths(key: String): List<VirtualFile> {
        val obj = this.asMap
        if (obj?.get(key) !is List<*>) return arrayListOf()
        val sourcePaths = obj[key] as List<*>

        return sourcePaths.mapNotNull { containingDirectory.findSubdirectory(it as String)?.virtualFile }
    }

    /**
     * Returns the collection of all schema.yml files found in the project
     */
    private val allSchemaFiles: List<DbtSchemaFile>
        get() {
            PsiManager.getInstance(projectYmlFile.project)
            val allSchemaFiles = FilenameIndex.getFilesByName(
                    projectYmlFile.project, "schema.yml", GlobalSearchScope.projectScope(projectYmlFile.project))
            return allSchemaFiles
                    .filter { psiFile -> DbtPsiUtil.isChildOf(containingDirectory, psiFile) && psiFile is YAMLFileImpl }
                    .map { psiFile -> DbtSchemaFile(psiFile as YAMLFileImpl) }
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
                .getAllFilesByExt(projectYmlFile.project, "sql")
                .filter { sqlFile ->
                    sourcePaths.any { sourcePath ->
                        VfsUtilCore.isAncestor(sourcePath, sqlFile, false)
                    }
                }.mapNotNull { PsiManager.getInstance(projectYmlFile.project).findFile(it) }
    }

    /**
     * Find a dbt source inside this dbt project module.
     *
     * @param sourceName: The first argument to source(), the name of the source schema
     * @param tableName: The second argument to source(), the name of the source table
     */
    fun findSourceTable(sourceName: String, tableName: String): PsiElement? {
        for (schemaFile in this.allSchemaFiles) {
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
        for (schemaFile in this.allSchemaFiles) {
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
        for (schemaFile in this.allSchemaFiles) {
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
        for (schemaFile in this.allSchemaFiles) {
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
    fun findAllMacroFiles(): List<PsiFile> {
        return FilenameIndex
                .getAllFilesByExt(projectYmlFile.project, "sql")
                .filter { sqlFile ->
                    macroPaths.any { sourcePath ->
                        VfsUtilCore.isAncestor(sourcePath, sqlFile, false)
                    }
                }.mapNotNull { PsiManager.getInstance(projectYmlFile.project).findFile(it) }
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
        result.addAll(DbtJinja2Functions.BUILTIN_FUNCTIONS)

        return result
    }
}
