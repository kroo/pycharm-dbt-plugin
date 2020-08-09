package com.getaround.pycharm.dbt

import com.getaround.pycharm.dbt.services.DbtContextMethod
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

class DbtJinja2BuiltinFunction(name: String, args: List<List<String>>, var documentationLink: String? = null)
    : DbtJinja2Function(name, args) {
    constructor(fn: DbtContextMethod) : this(fn.name, listOf(fn.args.map { it.name }))

}

//object DbtJinja2Functions {
//    val BUILTIN_FUNCTIONS: Array<DbtJinja2BuiltinFunction> = arrayOf(
//            DbtJinja2BuiltinFunction("ref", listOf(
//                    listOf("model_name"),
//                    listOf("package_name", "model_name")),
//                    "https://docs.getdbt.com/reference/dbt-jinja-functions/ref"),
//            DbtJinja2BuiltinFunction("source", listOf(
//                    listOf("source_name", "table_name")),
//                    "https://docs.getdbt.com/reference/dbt-jinja-functions/source"),
//            DbtJinja2BuiltinFunction("var", listOf(
//                    listOf("var_name"),
//                    listOf("var_name", "default")),
//                    "https://docs.getdbt.com/reference/dbt-jinja-functions/var")
//    )
//}
