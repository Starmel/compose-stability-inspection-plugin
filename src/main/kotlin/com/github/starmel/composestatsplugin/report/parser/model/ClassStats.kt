package com.github.starmel.composestatsplugin.report.parser.model

data class ClassStats(
    val name: String,
    val isStable: Boolean,
    val isRuntime: Boolean,
    val properties: List<Property>,
) {

    data class Property(
        val name: String,
        val type: String, // top level type, e.g. "Int", "List", "Publication?"
        val isMutable: Boolean,
        val isStable: Boolean,
        val isRuntime: Boolean,
    )
}