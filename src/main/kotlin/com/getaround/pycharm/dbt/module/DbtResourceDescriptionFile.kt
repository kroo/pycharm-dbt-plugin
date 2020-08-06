package com.getaround.pycharm.dbt.module

import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLSequenceItem
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLDocumentImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import org.jetbrains.yaml.psi.impl.YAMLSequenceImpl

class DbtResourceDescriptionFile(private val resourceDescriptionFile: YAMLFile) {
    private val document: YAMLDocumentImpl?
        get() {
            val document = resourceDescriptionFile.children[0]
            if (document !is YAMLDocumentImpl) return null
            return document
        }

    private val mapping: YAMLBlockMappingImpl?
        get() {
            val documentChildren = document?.children.orEmpty()
            if (documentChildren.isEmpty()) return null
            val mapping = document?.children?.get(0)
            if (mapping !is YAMLBlockMappingImpl) return null
            return mapping
        }

    private fun topLevelKey(name: String): YAMLSequenceImpl? {
        val sources = mapping?.getKeyValueByKey(name)
        if (sources?.value !is YAMLSequenceImpl) return null
        return sources.value as YAMLSequenceImpl
    }

    private val sources: YAMLSequenceImpl?
        get() = topLevelKey("sources")

    private val macros: YAMLSequenceImpl?
        get() = topLevelKey("macros")

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

    private fun getItemWithName(base: YAMLSequence?, name: String): YAMLSequenceItem? {
        val items = base?.items ?: listOf()
        for (item in items) {
            for (keyValue in item.keysValues) {
                if (keyValue.keyText == "name" && keyValue.valueText == name) {
                    return item
                }
            }
        }
        return null
    }

    fun getMacro(macroName: String): YAMLSequenceItem? = getItemWithName(macros, macroName)
    fun getSource(sourceName: String): YAMLSequenceItem? = getItemWithName(sources, sourceName)

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
