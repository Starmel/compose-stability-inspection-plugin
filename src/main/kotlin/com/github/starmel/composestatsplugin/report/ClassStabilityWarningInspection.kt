package com.github.starmel.composestatsplugin.report

import com.github.starmel.composestatsplugin.isComposableFunction
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.findParentOfType
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlinx.serialization.compiler.resolve.toClassDescriptor

class ClassStabilityWarningInspection : AbstractKotlinInspection() {

    override fun getDisplayName(): String {
        return "Unstable class used as Compose method argument"
    }

    override fun getShortName(): String {
        return "UnstableClass"
    }

    override fun getStaticDescription(): String {
        return "This method receives unstable arguments, potentially affecting Compose performance due to inefficient recompositions. Consider using stable types or annotating with @Stable"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {

        return object : PsiElementVisitor() {

            override fun visitElement(element: PsiElement) {

                val editor = FileEditorManager.getInstance(element.project)
                val stats = editor.project.service<StatsUpdateService>()

                if (element is KtClass) {
                    val identifier = element.identifyingElement ?: return
                    val name = element.name ?: return
                    val classReport = stats.classStats(name)

                    if (classReport != null && element.isUsedAsComposeMethodArgument()) {

                        val isStable = classReport.isStable || element.hasStableComposeAnnotation()

                        if (!isStable) {
                            holder.registerProblem(identifier, "Unstable class.")
                        }
                    }
                }
            }
        }
    }
}

fun KtClass.isUsedAsComposeMethodArgument(): Boolean {
    return ReferencesSearch.search(this).any { reference ->
        val namedFunction = reference.element.findParentOfType<KtNamedFunction>()
        return namedFunction != null && namedFunction.isComposableFunction()
    }
}

// TODO: Handle type aliases for @Stable @Immutable for Kotlin Multiplatform

fun KtClass.hasStableComposeAnnotation(): Boolean {
    return findAnnotation(FqName("androidx.compose.runtime.Stable")) != null ||
            findAnnotation(FqName("androidx.compose.runtime.Immutable")) != null
}

fun KotlinType.hasStableComposeAnnotation(): Boolean {
    return toClassDescriptor?.annotations?.run {
        hasAnnotation(FqName("androidx.compose.runtime.Stable")) ||
                hasAnnotation(FqName("androidx.compose.runtime.Immutable"))
    } ?: false
}