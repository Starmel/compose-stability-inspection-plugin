package com.github.starmel.composestatsplugin.report.parser

import com.github.starmel.composestatsplugin.report.parser.model.ClassStats

class ClassReportParser {

    fun parse(text: String): List<ClassStats> {

        val functions = mutableListOf<ClassStats>()
        val function = ClassStatsBuilder()


        val saveFunction = {
            functions.add(function.build())
            function.reset()
        }

        text.lines().forEach { line ->
            if (line.isBlank() || line.contains("<runtime stability>")) {
                return@forEach
            } else if (line.contains(" class ")) {
                function.isStable = !line.contains("unstable")
                function.name = try {
                    line.substringAfter(" class ").substringBefore(" {")
                } catch (e: Exception) {
                    error("Error parsing function name: $line")
                }
            } else {
                if (line.startsWith("}")) {
                    saveFunction.invoke()
                } else {
                    function.parameter {
                        isMutable = line.contains("var ")
                        isStable = line.contains(" stable ")
                        isRuntime = line.contains(" runtime ")
                        name = line.substringAfter(if (isMutable) "var " else "val ").substringBefore(": ")
                        type = line.substringAfter(": ").substringBefore("{")
                    }
                }
            }
        }

        return functions
    }

    class ClassStatsBuilder {
        var name: String = ""
        var isStable: Boolean = false
        var isRuntime: Boolean = false

        var properties = mutableListOf<ParameterBuilder>()

        fun parameter(parameter: ParameterBuilder.() -> Unit) {
            properties.add(ParameterBuilder().apply(parameter))
        }

        class ParameterBuilder {
            var name: String = ""
            var type: String = ""
            var isStable: Boolean = false
            var isMutable: Boolean = false
            var isRuntime: Boolean = false

            fun build(): ClassStats.Property {
                return ClassStats.Property(
                    name = name,
                    type = type,
                    isMutable = isMutable,
                    isStable = isStable,
                    isRuntime = isRuntime
                )
            }
        }

        fun List<ParameterBuilder>.build(): List<ClassStats.Property> {
            return map { it.build() }
        }

        fun build(): ClassStats {
            return ClassStats(
                name = name,
                isStable = isStable,
                isRuntime = isRuntime,
                properties = properties.build()
            )
        }

        fun reset() {
            name = ""
            isStable = false
            isRuntime = false
            properties = mutableListOf()
        }
    }
}