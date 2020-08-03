package com.getaround.pycharm.dbt.database

import com.intellij.database.settings.DatabaseParameterPatternProvider
import com.intellij.database.settings.UserPatterns.ParameterPattern

class DbtPatternProvider : DatabaseParameterPatternProvider {
    override fun getPatterns(): Array<ParameterPattern> {
        return ourPatterns
    }

    @Suppress("RegExpRedundantEscape")
    companion object {
        private val ourPatterns = arrayOf(
                ParameterPattern("\\{\\{(.*?)\\}\\}", "SQL", "{{name}}"))
    }
}
