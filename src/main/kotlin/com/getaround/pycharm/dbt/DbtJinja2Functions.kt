package com.getaround.pycharm.dbt

import DbtJinja2ExternalDocumentation.BUILTIN_OBJ_MAP
import com.getaround.pycharm.dbt.actions.getMapping
import com.getaround.pycharm.dbt.services.DbtContextMethod
import com.getaround.pycharm.dbt.services.DbtProjectService
import com.intellij.openapi.components.service
import com.jetbrains.jinja2.tags.Jinja2MacroTag

open class DbtJinja2Function(var name: String, var args: List<List<String>>) {
    open val doc: String? = null
    open val externalDocUrl: String? = null

}

class DbtJinja2Macro(
        val element: Jinja2MacroTag
) : DbtJinja2Function(
        element.name ?: "<missing name>",
        getArgs(element)) {

    override val doc: String?
        get() {
            val project = element.project.service<DbtProjectService>().findDbtProjectModule(element.containingFile)
            val macroName = element.name ?: return null
            val macroProperties = project?.findMacroProperties(macroName) ?: return null
            val description = macroProperties.getMapping()?.getKeyValueByKey("description")
            return description?.valueText
        }

    companion object {
        private fun getArgs(macroTag: Jinja2MacroTag): List<List<String>> {
            return listOf(macroTag.parameters.map { it.text ?: "<missing arg name>" })
        }
    }
}

class DbtJinja2BuiltinFunction(internal val ctxMethod: DbtContextMethod) :
        DbtJinja2Function(ctxMethod.name, argList(ctxMethod)) {
    override val doc: String? = ctxMethod.doc
    override val externalDocUrl: String? get() = BUILTIN_OBJ_MAP[ctxMethod.name]

    companion object {
        private fun argList(ctxMethod: DbtContextMethod): List<List<String>> {
            if (ctxMethod.name == "ref") {
                return listOf(
                        listOf("model_name"),
                        listOf("package_name", "model_name")
                )
            }
            if (ctxMethod.name == "var") {
                return listOf(
                        listOf("var_name"),
                        listOf("var_name", "default_value")
                )
            }
            return listOf(ctxMethod.args.map { it.name })
        }
    }
}
