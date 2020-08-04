package com.getaround.pycharm.dbt.listeners

import com.getaround.pycharm.dbt.services.DbtProjectService
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.sql.psi.SqlLanguage
import com.intellij.webcore.packaging.PackagesNotificationPanel
import com.jetbrains.jinja2.Jinja2Language
import com.jetbrains.python.PyBundle
import com.jetbrains.python.PyPsiPackageUtil
import com.jetbrains.python.configuration.PyConfigurableInterpreterList
import com.jetbrains.python.packaging.PyPackageManager
import com.jetbrains.python.packaging.PyPackageUtil
import com.jetbrains.python.packaging.ui.PyPackageManagementService
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.PythonSdkUtil
import com.jetbrains.python.sdk.pythonSdk
import com.jetbrains.python.sdk.setup
import com.jetbrains.python.templateLanguages.TemplatesService
import java.util.regex.Matcher
import java.util.regex.Pattern

internal class DbtProjectManagerListener : ProjectManagerListener {

    override fun projectOpened(project: Project) {
        val projectService = project.service<DbtProjectService>()
        // Configure template language to Jinja2 for any project with DBT
        val containsDbtProjects = ensureTemplateLanguageConfigured(projectService)

        // ensure dbt is installed in the python environment
        if (containsDbtProjects) {
            ensureDbtInstalled(project, projectService)
        }
    }

    private fun ensureDbtInstalled(project: Project, projectService: DbtProjectService) {
        val sdk = ensurePythonSdkConfigured(project, projectService)
        if (sdk != null) {
            ensureDbtPackageInstalled(project, sdk)
        }
    }

    private fun ensurePythonSdkConfigured(project: Project, projectService: DbtProjectService): Sdk? {
        var sdk = ProjectRootManager.getInstance(project).projectSdk
        if (sdk == null) {
            sdk = ensureProjectSdkAutoConfigured(project, projectService)

            if (sdk == null) {
                val configureInterpAction = ConfigureProjectInterpreterAction(project) { _ ->
                    ensurePythonSdkConfigured(project, projectService)
                }
                projectService
                        .notifyError("No Python SDK Configured! Please configure a python SDK for this project.")
                        .addAction(configureInterpAction)
                return null
            }
        }
        return sdk
    }

    private val shebangPattern = Pattern.compile("^#!(/[^ ]+)$")
    private fun ensureProjectSdkAutoConfigured(project: Project, projectService: DbtProjectService): Sdk? {
        val dbtFile = PathEnvironmentVariableUtil.findInPath("dbt")
        if (dbtFile == null) {
            println("Dbt command not found in path")
        }
        val firstLine = dbtFile?.readLines()?.firstOrNull()
        var matcher: Matcher? = shebangPattern.matcher(firstLine.orEmpty())
        if (matcher?.matches() != true) {
            println("Dbt binary does not start with shebang line, unable to detect python version")
            matcher = null
        }
        val pythonPath = matcher?.group(1)
        if (pythonPath == null || !PythonSdkType.getInstance().isValidSdkHome(pythonPath)) {
            println("Invalid SDK path found: $pythonPath")
            return null
        }
        val sdk = PyDetectedSdk(pythonPath)
        addAndSwitchToSdk(sdk, project, projectService)
        println("Switched to DBT in path: $dbtFile")
        return sdk
    }

    private fun addAndSwitchToSdk(sdk: PyDetectedSdk, project: Project, projectService: DbtProjectService) {
        val allSdks = PythonSdkUtil.getAllSdks()
        sdk.setup(allSdks + listOf(sdk))
        val model = PyConfigurableInterpreterList.getInstance(project).model
        model.addSdk(sdk)
        model.apply()
        (sdk.sdkType as PythonSdkType).setupSdkPaths(sdk)
        project.pythonSdk = sdk
        val findAllDbtModules = projectService.findAllDbtModules()
        for (it in findAllDbtModules) {
            // mark each module containing a dbt project file with the right template language configuration
            val module = ModuleUtil.findModuleForFile(it.projectYmlFile)
            module?.pythonSdk = sdk
        }
    }

    private fun ensureTemplateLanguageConfigured(projectService: DbtProjectService): Boolean {
        val findAllDbtModules = projectService.findAllDbtModules()
        for (it in findAllDbtModules) {
            // mark each module containing a dbt project file with the right template language configuration
            val module = ModuleUtil.findModuleForFile(it.projectYmlFile)
            val templatesService = TemplatesService.getInstance(module)
            templatesService.templateLanguage = Jinja2Language.INSTANCE.templateLanguageName
            templatesService.templateFileTypes = listOf(SqlLanguage.INSTANCE.associatedFileType?.name)
        }

        return findAllDbtModules.isNotEmpty()
    }

    private fun ensureDbtPackageInstalled(project: Project, sdk: Sdk) {
        // In the future, we should configure DBT version via dbt_project.yml...
        val requirement = "dbt"
        val forceInstallFramework = false
        val frameworkName = "dbt"
        val packageManager = PyPackageManager.getInstance(sdk)

        // From { com.jetbrains.python.newProject.PythonProjectGenerator }

        // Modal is used because it is insane to create project when framework is not installed
        ProgressManager.getInstance().run(object : Task.Modal(
                project, PyBundle.message("python.install.framework.ensure.installed", frameworkName), false) {
            override fun run(indicator: ProgressIndicator) {
                var installed = false
                if (!forceInstallFramework) {
                    // First check if we need to do it
                    indicator.text = PyBundle.message(
                            "python.install.framework.checking.is.installed", frameworkName)
                    val packages = PyPackageUtil.refreshAndGetPackagesModally(sdk)
                    installed = PyPsiPackageUtil.findPackage(packages, requirement) != null
                }
                if (!installed) {
                    indicator.text = PyBundle.message(
                            "python.install.framework.installing", frameworkName)
                    try {
                        packageManager.install(requirement)
                        packageManager.refresh()
                    } catch (e: ExecutionException) {
                        val errorDescription = PyPackageManagementService.toErrorDescription(listOf(e), sdk)
                        if (errorDescription == null) {
                            println("Unable to describe error $e")
                            return
                        }
                        val app = ApplicationManager.getApplication()
                        app.invokeLater {
                            PackagesNotificationPanel.showError(
                                    PyBundle.message(
                                            "python.new.project.install.failed.title",
                                            frameworkName),
                                    errorDescription)
                        }
                    }
                }
            }
        })
    }
}
