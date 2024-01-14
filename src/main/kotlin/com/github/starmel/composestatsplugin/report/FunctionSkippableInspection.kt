package com.github.starmel.composestatsplugin.report

import com.github.starmel.composestatsplugin.isComposableFunction
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.structuralsearch.resolveKotlinType
import org.jetbrains.kotlin.nj2k.types.typeFqName

class FunctionSkippableInspection : AbstractKotlinInspection() {

    override fun getDisplayName(): String {
        return "Unstable class used as Compose method argument"
    }

    override fun getStaticDescription(): String {
        return "This method receives unstable arguments, potentially affecting Compose performance due to inefficient recompositions. Consider using stable types or annotating with @Stable"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {

        return object : PsiElementVisitor() {

            private val kotlinPrimitiveSet = setOf(
                "Boolean",
                "Byte",
                "Char",
                "Short",
                "Int",
                "Float",
                "Double",
                "Long",
                "String",
                "Unit",
            )

            override fun visitElement(element: PsiElement) {

                val editor = FileEditorManager.getInstance(element.project)

                val stats = editor.project.service<StatsUpdateService>()

                if (element.isComposableFunction()) {

                    val name = element.name ?: return
                    val funStat = stats.functionStats(name)

                    element.valueParameters.forEach { parameterElement ->

                        val isStableByFunctionReport = funStat?.parameters
                            ?.find { param -> param.name == parameterElement.name }
                            ?.isStable
                            ?: false

                        val parameterTypeName = parameterElement.typeFqName()?.shortName()?.asString() ?: return
                        val isStableByClassReport = stats.classStats(parameterTypeName)?.isStable ?: false

                        val hasStableAnnotation = parameterElement.resolveKotlinType()
                            ?.hasStableComposeAnnotation()
                            ?: false

                        val isPrimitive = parameterElement.typeReference?.text?.let { typeReference ->
                            kotlinPrimitiveSet.contains(typeReference)
                        } ?: false

                        val isStable =
                            isStableByFunctionReport || isStableByClassReport || hasStableAnnotation || isPrimitive

                        if (!isStable) {
                            val typeReference = parameterElement.typeReference ?: return
                            holder.registerProblem(typeReference, "Unstable Argument. $staticDescription")
                        }
                    }
                }
            }
        }
    }
}
