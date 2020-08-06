package com.getaround.pycharm.dbt.module

import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLSequenceItem
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLDocumentImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import org.jetbrains.yaml.psi.impl.YAMLSequenceImpl

class DbtSchemaFile(private val schemaFile: YAMLFile) {
    private val document: YAMLDocumentImpl?
        get() {
            val document = schemaFile.children[0]
            if (document !is YAMLDocumentImpl) return null
            return document
        }

    private val mapping: YAMLBlockMappingImpl?
        get() {
            val mapping = document?.children?.get(0)
            if (mapping !is YAMLBlockMappingImpl) return null
            return mapping
        }

    private val sources: YAMLSequenceImpl?
        get() {
            val sources = mapping?.getKeyValueByKey("sources")
            if (sources?.value !is YAMLSequenceImpl) return null
            return sources.value as YAMLSequenceImpl
        }

    fun getAllSources(): Collection<YAMLSequenceItem> {
        val allSources = ArrayList<YAMLSequenceItem>()
        val items = sources?.items ?: return allSources
        for (source in items) {
            for (keyValue in source.keysValues) {
                if (keyValue.keyText == "name") {
                    allSources.add(source)
                }
            }
        }
        return allSources
    }

    fun getSource(sourceName: String): YAMLSequenceItem? {
        val items = sources?.items ?: listOf()
        for (source in items) {
            for (keyValue in source.keysValues) {
                if (keyValue.keyText == "name" && keyValue.valueText == sourceName) {
                    return source
                }
            }
        }
        return null
    }

    fun getAllSourceTables(sourceName: String): Collection<YAMLPlainTextImpl> {
        val allSources = ArrayList<YAMLPlainTextImpl>()
        forAllSourceTableKeyValues(sourceName) { kv ->
            if (kv.keyText == "name" && kv.value is YAMLPlainTextImpl) {
                allSources.add(kv.value as YAMLPlainTextImpl)
            }

            true
        }
        return allSources
    }

    fun getSourceTable(sourceName: String, tableName: String): PsiElement? {
        var result: PsiElement? = null
        forAllSourceTableKeyValues(sourceName) { kv ->
            if (kv.keyText == "name" && kv.valueText == tableName) {
                result = kv.value
                false
            } else true
        }
        return result
    }

    /**
     * Helper function to iterate over all yaml table k/v items for a given source name
     */
    private fun forAllSourceTableKeyValues(sourceName: String, kvProcessor: (YAMLKeyValue) -> Boolean) {
        val sourceKeysVals = getSource(sourceName)?.keysValues ?: listOf()
        for (keyValue in sourceKeysVals) {
            if (keyValue.keyText != "tables" || keyValue.value !is YAMLSequenceImpl) continue
            val tableItems = (keyValue.value as YAMLSequenceImpl).items
            for (tableItem in tableItems) {
                var continueIterating = true
                for (tableItemKeyValue in tableItem.keysValues) {
                    continueIterating = continueIterating && kvProcessor(tableItemKeyValue)
                }
                if (!continueIterating) return
            }
        }
    }
}
