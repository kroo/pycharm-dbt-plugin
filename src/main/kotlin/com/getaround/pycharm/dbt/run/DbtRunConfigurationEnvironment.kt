package com.getaround.pycharm.dbt.run

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.python.run.PythonCommandLineState
import java.nio.file.Path
import java.nio.file.Paths

class DbtRunConfigurationEnvironment(
        private val runConfiguration: DbtRunConfiguration,
        private val env: ExecutionEnvironment) :
        PythonCommandLineState(runConfiguration, env) {
    override fun generateCommandLine(): GeneralCommandLine {
        val commandLine = super.generateCommandLine()
        val projectRoot: Path = Paths.get(runConfiguration.projectRoot)
        commandLine.workDirectory = projectRoot.toFile()
        commandLine.addParameters("-c", "import dbt.main; dbt.main.main()", runConfiguration.command)
        commandLine.addParameters(runConfiguration.args.split(" "))

        return commandLine
    }
}