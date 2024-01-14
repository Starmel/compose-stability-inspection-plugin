package com.github.starmel.composestatsplugin.report.parser.model

data class FunctionStats(
    val name: String,
    val parameters: List<Parameter>,
    val isRestartable: Boolean,
    val isSkippable: Boolean,
    val isReadOnly: Boolean,
    val returnType: String?,
    val isGetterFunction: Boolean,
) {

    data class Parameter(
        val name: String,
        val isStable: Boolean,
        val isThis: Boolean,
        val isUnused: Boolean,
    )
}