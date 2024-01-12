package com.github.starmel.composestatsplugin.way

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
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
                println("expression: ${element.javaClass.simpleName}")
                if (element.javaClass.simpleName == "KtLiteralStringTemplateEntry") {

                    val psiElement = element

                    val length = element.text.length

                    sink.addInlineElement(
                        offset = psiElement.textRange.endOffset,
                        relatesToPrecedingText = true,
                        presentation = factory.text(" $length chars"),
                        placeAtTheEndOfLine = false
                    )

                    sink.addCodeVisionElement(
                        editor = editor,
                        offset = psiElement.textRange.endOffset,
                        priority = 0,
                        presentation = factory.text("$length chars *")
                    )
                }
                return true
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
