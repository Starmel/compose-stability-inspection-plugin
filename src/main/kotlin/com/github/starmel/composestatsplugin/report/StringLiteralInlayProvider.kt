package com.github.starmel.composestatsplugin.report

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import javax.swing.JComponent
import javax.swing.JLabel

class StringLiteralInlayProvider : InlayHintsProvider<NoSettings> {

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(
                element: PsiElement,
                editor: Editor,
                sink: InlayHintsSink
            ): Boolean {
                val stats = editor.project?.service<StatsUpdateService>() ?: return false

                if (element is KtNamedFunction && hasComposableAnnotation(element)) {

                    val composableAnnotation =
                        element.annotationEntries.find { it.shortName?.identifier == "Composable" }
                            ?: return false.also {
                                thisLogger().error("No composable annotation found for ${element.name}")
                            }

                    val name = element.name ?: return false
                    val funStat = stats.functionStats(name) ?: return false.also {
                        thisLogger().error("No stats found for ${element.name}")
                    }

                    sink.addCodeVisionElement(
                        editor = editor,
                        offset = composableAnnotation.textOffset,
                        priority = 0,
                        presentation = factory.text("Skippable: ${funStat.isSkippable} Restartable: ${funStat.isRestartable} ReadOnly: ${funStat.isReadOnly}")
                    )

//                    element.valueParameters.forEach { parameterElement ->
//
//                        val paramStat = funStat.parameters.find { param -> param.name == parameterElement.name }
//                            ?: return false.also {
//                                thisLogger().error("No stats found for ${element.name}}")
//                            }
//
//                        sink.addInlineElement(
//                            offset = parameterElement.textOffset - 1,
//                            relatesToPrecedingText = true,
//                            presentation = factory.roundWithBackgroundAndSmallInset(
//                                factory.text(if (paramStat.isStable) "Stable" else "Unstable"),
//                            ),
//                            placeAtTheEndOfLine = false
//                        )
//                    }
                } else {
//                    println("Element type: ${element.javaClass.name} - ${element.toString()}")
                }
                return true
            }

            fun hasComposableAnnotation(function: KtFunction): Boolean {
                return function.annotationEntries.any { it.shortName?.identifier == "Composable" }
            }
        }
    }

    override val key: SettingsKey<NoSettings>
        get() = SettingsKey("StringLiteralInlayProvider")
    override val name: String
        get() = "StringLiteralInlayProvider"
    override val previewText: String?
        get() = "StringLiteralInlayProvider preview text"

    override fun createSettings(): NoSettings {
        return NoSettings()
    }

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent {
                return JLabel("No settings")
            }

            override fun reset() {
            }
        }
    }
}
