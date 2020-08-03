package com.getaround.pycharm.dbt

import com.jetbrains.jinja2.tags.Jinja2MacroTag

open class DbtJinja2Function(var name: String, var args: List<List<String>>) {
    val minArity: Int get() = args.minBy { it.size }?.size ?: 0
    val maxArity: Int get() = args.maxBy { it.size }?.size ?: 0
}

class DbtJinja2Macro(
    val element: Jinja2MacroTag
) : DbtJinja2Function(
        element.name ?: "<missing name>",
        getArgs(element)) {

    companion object {
        fun getArgs(macroTag: Jinja2MacroTag): List<List<String>> {
            return listOf(macroTag.parameters.map { it.text ?: "<missing name>" })
        }
    }
}

object DbtJinja2Functions {
    val BUILTIN_FUNCTIONS = arrayOf(
            DbtJinja2Function("ref", listOf(
                    listOf("model_name"),
                    listOf("package_name", "model_name"))),
            DbtJinja2Function("source", listOf(
                    listOf("source_name", "table_name")
            )),
            DbtJinja2Function("var", listOf(
                    listOf("var_name"),
                    listOf("var_name", "default")
            ))
    )
}
