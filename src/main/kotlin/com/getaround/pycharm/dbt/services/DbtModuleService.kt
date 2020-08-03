package com.getaround.pycharm.dbt.services

import com.intellij.openapi.module.Module

class DbtModuleService(module: Module) {
    init {
        println(module.moduleFilePath)
    }
}
