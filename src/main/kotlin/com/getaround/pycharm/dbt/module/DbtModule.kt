package com.getaround.pycharm.dbt.module

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.yaml.psi.impl.YAMLFileImpl
import org.yaml.snakeyaml.Yaml

class DbtModule(val projectYmlFile: PsiFile) {
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
        get() {
            val obj = this.asMap
            if (obj?.get("source-paths") !is List<*>) return arrayListOf()
            val sourcePaths = obj["source-paths"] as List<*>

            return sourcePaths.mapNotNull { containingDirectory.findSubdirectory(it as String)?.virtualFile }
        }

    private val allSchemaFiles: List<DbtSchemaFile>
        get() {
            PsiManager.getInstance(projectYmlFile.project)
            val allSchemaFiles = FilenameIndex.getFilesByName(
                    projectYmlFile.project, "schema.yml", GlobalSearchScope.projectScope(projectYmlFile.project))
            return allSchemaFiles
                    .filter { psiFile -> isChildOf(containingDirectory, psiFile) && psiFile is YAMLFileImpl }
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

    private fun isChildOf(directory: PsiDirectory, file: PsiFile): Boolean {
        var parent = file.parent
        while (parent != null) {
            if (directory == parent) {
                return true
            }
            parent = parent.parentDirectory
        }
        return false
    }
}
