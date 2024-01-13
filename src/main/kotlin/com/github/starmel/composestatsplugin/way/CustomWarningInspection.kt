package com.github.starmel.composestatsplugin.way

import com.github.starmel.composestatsplugin.StatsUpdateService
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.KtNamedFunction
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class CustomWarningInspection : AbstractKotlinInspection() {

    override fun getStaticDescription(): String {
        return "This method receives unstable arguments, potentially affecting Compose performance due to inefficient recompositions. Consider using stable types or annotating with @Stable"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {

        return object : PsiElementVisitor() {

            override fun visitElement(element: PsiElement) {

                val editor = FileEditorManager.getInstance(element.project)

                val stats = editor.project.service<StatsUpdateService>()

                if (element.isComposableFunction()) {

                    val funStat = stats.getStats()[element.name] ?: return Unit.also {
                        thisLogger().error("No stats found for ${element.name}")
                    }

                    element.valueParameters.forEach { parameterElement ->

                        val paramStat = funStat.parameters
                            .find { param -> param.name == parameterElement.name }
                            ?: return Unit.also {
                                thisLogger().error("No stats found for ${element.name}}")
                            }

                        if (!paramStat.isStable) {
                            holder.registerProblem(parameterElement, "Unstable Argument.")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalContracts::class)
fun PsiElement.isComposableFunction(): Boolean {

    contract {
        returns(true) implies (this@isComposableFunction is KtNamedFunction)
    }

    return this is KtNamedFunction && this.annotationEntries.any { it.shortName?.identifier == "Composable" }
}
