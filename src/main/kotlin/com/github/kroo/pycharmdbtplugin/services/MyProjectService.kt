package com.github.kroo.pycharmdbtplugin.services

import com.intellij.openapi.project.Project
import com.github.kroo.pycharmdbtplugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
