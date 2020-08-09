package com.getaround.pycharm.dbt.services

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.util.LocalTimeCounter
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.pyi.PyiFileType
import com.jetbrains.python.sdk.PySdkUtil
import com.jetbrains.python.sdk.pythonSdk

class DbtTypeService(val project: Project) {
    var builtinTypeJson: JsonElement? = null
    val contextsPythonScript = String(javaClass
            .getResourceAsStream("/python/collect-dbt-contexts.py")
            .readAllBytes())

    fun collectDbtContexts() {
        ProgressManager.getInstance().run(object
            : Task.Backgroundable(project, "Collecting dbt contexts") {
            override fun run(indicator: ProgressIndicator) {
                val sdk = project.pythonSdk ?: return

                val output = PySdkUtil.getProcessOutput(
                        project.basePath, arrayOf(sdk.name), mapOf(),
                        10000, contextsPythonScript.toByteArray(), false)
                val stdOut = output.stdout
                builtinTypeJson = JsonParser.parseString(stdOut)
            }
        })

    }

    val builtinFunctions: List<DbtContextMethod>
        get() = builtinTypes.filterIsInstance<DbtContextMethod>()

    val builtin: List<DbtContextValue>
        get() = builtinTypes.filterIsInstance<DbtContextValue>()

    val builtinTypes: List<DbtBuiltinType>
        get() {
            val result = mutableListOf<DbtBuiltinType>()
            if (builtinTypeJson !is JsonObject) return result.toList()
            val allTypes = setOf("base", "model", "target", "macro").flatMap { context ->
                (builtinTypeJson as JsonObject).get(context).asJsonArray.map { contextElement ->
                    val contextObject = contextElement.asJsonObject
                    val nameArg = contextObject.get("name")?.asString
                    val docArg = contextObject.get("doc")?.asString
                    val valueArg = contextObject.get("value")?.asString
                    val argsArg = contextObject.get("args")
                    val resultArg = contextObject.get("result")?.asString

                    when {
                        (valueArg == null) -> DbtContextMethod(nameArg.orEmpty(), argsArg?.asJsonArray?.map {
                            DbtMethodArgument(it.asJsonObject.get("name").asString, it.asJsonObject.get("value").asString)
                        }.orEmpty(), resultArg.orEmpty(), docArg)
                        else -> DbtContextValue(nameArg.orEmpty(), valueArg.orEmpty(), docArg)
                    }
                }
            }
            result.addAll(allTypes)
            return result.toList()
        }
}

interface DbtBuiltinType {
    val name: String
}

data class DbtContextValue(override val name: String, val value: String, val doc: String?) : DbtBuiltinType
data class DbtContextMethod(override val name: String, val args: List<DbtMethodArgument>, val result: String, val doc: String?) : DbtBuiltinType
data class DbtMethodArgument(override val name: String, val value: String) : DbtBuiltinType

fun DbtContextValue.resolveValueType(project: Project): PyElement? {
    return PsiFileFactory.getInstance(project)
            .createFileFromText("temp." + PyiFileType.INSTANCE.defaultExtension,
                    PyiFileType.INSTANCE, this.value,
                    LocalTimeCounter.currentTime(),
                    true).children[0] as PyElement
}

fun DbtMethodArgument.resolveValueType(project: Project): PyElement? {
    return PsiFileFactory.getInstance(project)
            .createFileFromText("temp." + PyiFileType.INSTANCE.defaultExtension,
                    PyiFileType.INSTANCE, this.value,
                    LocalTimeCounter.currentTime(),
                    true).children[0] as PyElement
}