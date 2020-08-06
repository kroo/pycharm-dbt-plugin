package com.getaround.pycharm.dbt.listeners

import com.getaround.pycharm.dbt.services.DbtProjectService
import com.intellij.execution.ExecutionException
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.webcore.packaging.PackagesNotificationPanel
import com.jetbrains.python.PyBundle
import com.jetbrains.python.PyPsiPackageUtil
import com.jetbrains.python.packaging.PyPIPackageUtil
import com.jetbrains.python.packaging.PyPackage
import com.jetbrains.python.packaging.PyPackageManager
import com.jetbrains.python.packaging.PyPackageUtil
import com.jetbrains.python.packaging.ui.PyPackageManagementService
import com.jetbrains.python.sdk.pythonSdk

class UpdateDBTVersionAction(
        val project: Project,
        val callback: (p: PyPackage?) -> Unit = {})
    : AnAction(actionText) {

    companion object {
        val DBT_PYPACKAGE_NAME = "dbt"
        private val actionText: String = "Update DBT"
    }

    override fun update(e: AnActionEvent) {
        e.presentation.text = actionText
        e.presentation.description = actionText
        e.presentation.isEnabledAndVisible = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val message = PyBundle.message("python.install.framework.ensure.installed", DBT_PYPACKAGE_NAME)
        ProgressManager.getInstance().run(object : Task.Modal(project, message, false) {
            override fun run(indicator: ProgressIndicator) {
                val projectService = project.service<DbtProjectService>()
                val pythonSdk = project.pythonSdk ?: return
                val packageManager = PyPackageManager.getInstance(pythonSdk)

                // First check if we need to do it
                indicator.text = PyBundle.message(
                        "python.install.framework.checking.is.installed", DBT_PYPACKAGE_NAME)
                var packages = PyPackageUtil.refreshAndGetPackagesModally(pythonSdk)
                var pyPackage = PyPsiPackageUtil.findPackage(packages, DBT_PYPACKAGE_NAME)
                val version = PyPIPackageUtil.INSTANCE.fetchLatestPackageVersion(project, DBT_PYPACKAGE_NAME)

                if (pyPackage == null) {
                    projectService.notifyError("Package was not found, please install this package first")
                    callback(null)
                    return
                }

                if (version == null) {
                    projectService.notifyError("Unable to determine new version to install")
                    callback(null)
                    return
                }

                indicator.text = PyBundle.message(
                        "python.install.framework.installing", DBT_PYPACKAGE_NAME)
                try {
                    packageManager.install("$DBT_PYPACKAGE_NAME==$version")
                    packageManager.refresh()
                } catch (e: ExecutionException) {
                    val errorDescription = PyPackageManagementService.toErrorDescription(listOf(e), pythonSdk)
                    if (errorDescription == null) {
                        println("Unable to describe error $e")
                        callback(null)
                        return
                    }
                    val app = ApplicationManager.getApplication()
                    app.invokeLater {
                        PackagesNotificationPanel.showError(
                                PyBundle.message(
                                        "python.new.project.install.failed.title",
                                        DBT_PYPACKAGE_NAME),
                                errorDescription)
                    }
                }

                packages = PyPackageUtil.refreshAndGetPackagesModally(pythonSdk)
                pyPackage = PyPsiPackageUtil.findPackage(packages, DBT_PYPACKAGE_NAME)

                callback(pyPackage)
            }
        })
    }

}
