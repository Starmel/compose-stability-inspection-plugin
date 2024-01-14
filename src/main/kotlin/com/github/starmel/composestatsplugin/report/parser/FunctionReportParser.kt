package com.github.starmel.composestatsplugin.report.parser

import com.github.starmel.composestatsplugin.report.parser.model.FunctionStats

class FunctionReportParser {

    fun parse(text: String): List<FunctionStats> {

        val functions = mutableListOf<FunctionStats>()
        val function = FunctionBuilder()

        var isFunDeclarationLine = true

        val saveFunction = {
            functions.add(function.build())
            function.reset()
            isFunDeclarationLine = true
        }

        text.lines().forEach { line ->
            if (line.isBlank()) {
                return@forEach
            } else if (isFunDeclarationLine) {
                isFunDeclarationLine = false
                function.isSkippable = line.contains("skippable")
                function.isRestartable = line.contains("restartable")
                function.isReadOnly = line.contains("readonly")
                function.name = try {
                    line.substring(line.indexOf("fun") + 3, line.lastIndexOf("(")).trim()
                } catch (e: Exception) {
                    error("Error parsing function name: $line")
                }

                if (function.name.startsWith("<get-")) {
                    function.isGetterFunction = true
                    function.name = function.name
                        .removePrefix("<get-")
                        .removeSuffix(">")
                }
                if (line.endsWith("()")) {
                    saveFunction.invoke()
                }
            } else {
                if (line.startsWith(")")) {
                    if (line.startsWith("):")) {
                        function.returnType = line.substringAfter("): ")
                    }
                    saveFunction.invoke()
                } else {
                    function.parameter {
                        val args = line.trim().split(Regex("[\\s+!:]")).take(2)

                        isStable = args.contains("stable")
                        isThis = line.contains("<this>")

                        if (isThis) {
                            isUnused = line.contains("unused")
                        } else {
                            name = args[1]
                        }
                    }
                }
            }
        }

        return functions
    }

    class FunctionBuilder {
        var isRestartable: Boolean = false
        var isSkippable: Boolean = false
        var isReadOnly: Boolean = false
        var name: String = ""
        var isGetterFunction: Boolean = false

        var parameters = mutableListOf<ParameterBuilder>()

        var returnType: String? = null

        fun parameter(parameter: ParameterBuilder.() -> Unit) {
            parameters.add(ParameterBuilder().apply(parameter))
        }

        class ParameterBuilder {
            var name: String = ""
            var isStable: Boolean = false
            var isThis: Boolean = false
            var isUnused: Boolean = false

            fun build(): FunctionStats.Parameter {
                return FunctionStats.Parameter(name, isStable, isThis, isUnused)
            }
        }

        fun List<ParameterBuilder>.build(): List<FunctionStats.Parameter> {
            return map { it.build() }
        }

        fun build(): FunctionStats {
            return FunctionStats(
                name,
                parameters.build(),
                isRestartable,
                isSkippable,
                isReadOnly,
                returnType,
                isGetterFunction
            )
        }

        fun reset() {
            name = ""
            isRestartable = false
            isSkippable = false
            isReadOnly = false
            returnType = null
            isGetterFunction = false
            parameters = mutableListOf()
        }
    }
}