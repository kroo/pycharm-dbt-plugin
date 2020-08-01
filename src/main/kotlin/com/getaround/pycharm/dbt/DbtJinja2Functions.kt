package com.getaround.pycharm.dbt


data class DbtJinja2Function(var name: String, var args: List<List<String>>) {
    val minArity: Int get() = args.minBy { it.size }?.size ?: 0
    val maxArity: Int get() = args.maxBy { it.size }?.size ?: 0
}

object DbtJinja2Functions {
    val BUILTIN_FUNCTION_NAMES = arrayOf(
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