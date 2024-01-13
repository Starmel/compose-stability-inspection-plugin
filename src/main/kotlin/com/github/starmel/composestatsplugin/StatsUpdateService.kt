package com.github.starmel.composestatsplugin

import com.github.starmel.composestatsplugin.parser.FunctionReportParser
import com.github.starmel.composestatsplugin.parser.model.FunctionStats
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
    private val parser = FunctionReportParser()

    private var stats = mutableMapOf<String, FunctionStats>()

    var didReadStats = false

    init {
        // TODO : Close the subscription when the project is closed
        project.messageBus.connect()
            .subscribe(VirtualFileManager.VFS_CHANGES, this)
    }

    fun getStats(): MutableMap<String, FunctionStats> {

        if (!didReadStats) {
            val file = LocalFileSystem.getInstance()
                .findFileByNioFile(Path.of(project.basePath, "compose_metrics/app_debug-composables.txt"))

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
                    refreshStatsFromFile(event.file)
                } else {
//                    println("File changed: ${event.file.name}")
                }
            }
        }
    }

    private fun refreshStatsFromFile(file: VirtualFile) {
        println("Refresh stats")
        val content = file.inputStream.bufferedReader().readText()
        this.stats = parser.parse(content).associateBy { it.name }.toMutableMap()
        didReadStats = true
    }
}

