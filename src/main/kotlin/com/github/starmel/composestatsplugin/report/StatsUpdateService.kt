package com.github.starmel.composestatsplugin.report

import com.github.starmel.composestatsplugin.report.parser.ClassReportParser
import com.github.starmel.composestatsplugin.report.parser.FunctionReportParser
import com.github.starmel.composestatsplugin.report.parser.model.ClassStats
import com.github.starmel.composestatsplugin.report.parser.model.FunctionStats
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.nio.file.Path

@Service
class StatsUpdateService(
    private val project: Project
) : BulkFileListener {

    private val functionParser = FunctionReportParser()
    private val classParser = ClassReportParser()

    private var functionStatsMap = mutableMapOf<String, FunctionStats>()
    private var classStatsMap = mutableMapOf<String, ClassStats>()

    private var didReadFunctionStats = false
    private var didReadClassStats = false

    init {
        // TODO : Close the subscription when the project is closed
        project.messageBus.connect()
            .subscribe(VirtualFileManager.VFS_CHANGES, this)
    }

    fun functionStats(name: String): FunctionStats? {
        readFunctionMetricsIfNeed()
        return functionStatsMap[name]
    }

    fun classStats(name: String): ClassStats? {
        readClassMetricsIfNeed()
        return classStatsMap[name]
    }

    private fun readFunctionMetricsIfNeed() {
        if (!didReadFunctionStats) {
            val file = LocalFileSystem.getInstance()
                .findFileByNioFile(Path.of(project.basePath, "compose_metrics/app_debug-composables.txt"))
            if (file != null) {
                refreshFunctionStatsFromFile(file)
            }
        }
    }

    private fun readClassMetricsIfNeed() {
        if (!didReadClassStats) {
            val file = LocalFileSystem.getInstance()
                .findFileByNioFile(Path.of(project.basePath, "compose_metrics/app_debug-classes.txt"))
            if (file != null) {
                refreshClassStatsFromFile(file)
            }
        }
    }

    override fun after(events: MutableList<out VFileEvent>) {
        events.forEach { event ->
            if (event is VFileContentChangeEvent) {
                when (event.file.name) {
                    "app_debug-composables.txt" -> refreshFunctionStatsFromFile(event.file)
                    "app_debug-classes.txt" -> refreshFunctionStatsFromFile(event.file)
                    else -> Unit
                }
            }
        }
    }

    private fun refreshFunctionStatsFromFile(file: VirtualFile) {
        val content = file.inputStream.bufferedReader().readText()
        this.functionStatsMap = functionParser.parse(content).associateBy { it.name }.toMutableMap()
        didReadFunctionStats = true
    }

    private fun refreshClassStatsFromFile(file: VirtualFile) {
        val content = file.inputStream.bufferedReader().readText()
        this.classStatsMap = classParser.parse(content).associateBy { it.name }.toMutableMap()
        didReadClassStats = true
    }
}

