package com.github.starmel.composestatsplugin

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import javax.swing.JComponent
import javax.swing.JLabel

class MyInlayHintsProvider : InlayHintsProvider<Unit> {

    override val key: SettingsKey<Unit>
        get() = SettingsKey("MyInlayHintsProvider")

    override val name: String
        get() = "MyInlayHintsProvider"

    override val previewText: String?
        get() = "This is a preview text"


    override fun createSettings() {

    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Unit,
        sink: InlayHintsSink
    ): InlayHintsCollector? {
        return object : FactoryInlayHintsCollector(editor) {

            init {
                println("Create collector for file: ${file.name}")
            }

            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {

                val stats = editor.project?.service<StatsUpdateService>() ?: return false

                println("Element type: ${element.javaClass.name} - ${element.toString()} ?? = ${KtNamedFunction::class.java.name}")

                if (element is KtNamedFunction && hasComposableAnnotation(element)) {

                    element.valueParameters.forEach {

                        println("Kt Parameter: ${it.text}")

                        sink.addInlineElement(
                            it.textOffset - 1,
                            true,
                            factory.roundWithBackgroundAndSmallInset(
                                factory.text("Param: ${it.text}")
                            ),
                            false
                        )
                    }
                } else {
//                    println("Element type: ${element.javaClass.name} - ${element.toString()}")
                }

                return true
            }

            fun hasComposableAnnotation(function: KtFunction): Boolean {
                val composableImport = "androidx.compose.runtime.Composable"
                for (annotation in function.annotationEntries) {
                    val annotationText = annotation.shortName?.identifier
                    if (annotationText == composableImport) {
                        return true
                    } else {
                        println("Annotation: $annotationText - $composableImport")
                    }
                }
                return false
            }
        }
    }

    override fun createConfigurable(settings: Unit): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent {
                return JLabel("This is a configurable")
            }
        }
    }
}