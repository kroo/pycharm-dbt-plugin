package com.getaround.pycharm.dbt.module

import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLSequenceItem
import org.jetbrains.yaml.psi.impl.*

class DbtSchemaFile(private val schemaFile: YAMLFileImpl) {
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

    fun getAllSources() : Collection<YAMLSequenceItem> {
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
        val items = sources?.items ?: return null
        for (source in items) {
            for (keyValue in source.keysValues) {
                if (keyValue.keyText == "name" && keyValue.valueText == sourceName) {
                    return source
                }
            }
        }
        return null
    }

    fun getAllSourceTables(sourceName: String) : Collection<YAMLPlainTextImpl> {
        val allSources = ArrayList<YAMLPlainTextImpl>()
        val source = getSource(sourceName) ?: return allSources
        for (keyValue in source.keysValues) {
            if (keyValue.keyText == "tables") {
                val tables = keyValue.value ?: continue
                if (tables !is YAMLSequenceImpl) continue
                val tableItems = tables.items
                for (tableItem in tableItems) {
                    for (tableItemKeyValue in tableItem.keysValues) {
                        if (tableItemKeyValue.keyText == "name" && tableItemKeyValue.value is YAMLPlainTextImpl) {
                            allSources.add(tableItemKeyValue.value as YAMLPlainTextImpl)
                        }
                    }
                }
            }
        }
        return allSources
    }
    fun getSourceTable(sourceName: String, tableName: String) : PsiElement? {
        val source = getSource(sourceName) ?: return null
        for (keyValue in source.keysValues) {
            if (keyValue.keyText == "tables") {
                val tables = keyValue.value ?: return null
                if (tables !is YAMLSequenceImpl) return null
                val tableItems = tables.items
                for (tableItem in tableItems) {
                    for (tableItemKeyValue in tableItem.keysValues) {
                        if (tableItemKeyValue.keyText == "name" && tableItemKeyValue.valueText == tableName) {
                            return tableItemKeyValue.value
                        }
                    }
                }
            }
        }
        return null
    }
}