package com.github.starmel.composestatsplugin

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

@Service
class StatsUpdateService(
    private val project: Project
) : BulkFileListener {

   private var stats: List<ComposableFunctionStats> = emptyList()

    var didReadStats = false

    init {
        // TODO : Close the subscription when the project is closed
        project.messageBus.connect()
            .subscribe(VirtualFileManager.VFS_CHANGES, this)
    }

    fun getStats(): List<ComposableFunctionStats> {

        if (!didReadStats){
            val file = LocalFileSystem.getInstance().findFileByPath("compose_metrics/app_debug-composables.txt")
            if (file != null) {
                refreshStatsFromFile(file)
            }
        }

        return stats
    }

    override fun after(events: MutableList<out VFileEvent>) {
        events.forEach { event ->
            if (event is VFileContentChangeEvent) {
                if (event.file.name == "app_debug-composables.txt") {
                    println("MainActivity.kt changed")
                    refreshStatsFromFile(event.file)
                } else {
                    println("File changed: ${event.file.name}")
                }
            }
        }
    }

    private fun refreshStatsFromFile(file: VirtualFile) {
        println("Refresh stats")

        val content = file.inputStream.bufferedReader().readText()
        val functionsRaw = content.split(Regex("\\(\\n"))

        val stats = functionsRaw.map(::parseFunctionStats)

        stats.forEach {
            println("Function: ${it.name}")
            it.parameters.forEach { param ->
                println("   Param: ${param.name} - ${param.typeRaw} - ${param.isStable}")
            }
        }

        didReadStats = true
        this.stats = stats
    }

    private fun parseFunctionStats(functionText: String): ComposableFunctionStats {
        val isRestartable = "restartable" in functionText
        val isSkippable = "skippable" in functionText
        val isReadOnly = !isRestartable && !isSkippable

        val functionNameRegex = """fun\s+(\w+)\(""".toRegex(RegexOption.IGNORE_CASE)
        val functionName = functionNameRegex.find(functionText)?.groupValues?.get(1) ?: ""

        val paramsRegex = """(stable|unstable)?\s*(\w+):\s*([\w<>, ]+)""".toRegex(RegexOption.IGNORE_CASE)
        val parameters = paramsRegex.findAll(functionText).map { result ->
            val (stability, name, type) = result.destructured
            ComposableFunctionStats.Parameter(name.trim(), type.trim(), stability == "stable")
        }.toList()

        return ComposableFunctionStats(functionName, parameters, isRestartable, isSkippable, isReadOnly)
    }
}

data class ComposableFunctionStats(
    val name: String,
    val parameters: List<Parameter>,
    val isRestartable: Boolean,
    val isSkippable: Boolean,
    val isReadOnly: Boolean,
) {

    data class Parameter(
        val name: String,
        val typeRaw: String, // Like: "String", "Int", "List<String>", "StateFlow<List<String>>"
        val isStable: Boolean,
    )
}