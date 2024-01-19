package com.github.starmel.composestatsplugin.fly

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedFunction

class OnFlyMethodStabilityInspection : AbstractKotlinInspection() {

    override fun getDisplayName(): String {
        return "Unstable type used as Compose method argument"
    }

    override fun getStaticDescription(): String? {
        return "Unstable type used as Compose method argument"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {

        return object : PsiElementVisitor() {

            override fun visitElement(element: PsiElement) {
                super.visitElement(element)

                if (element is KtNamedFunction) {

                    val hasComposableAnnotation =
                        element.findAnnotation(FqName("androidx.compose.runtime.Composable")) != null

                    if (!hasComposableAnnotation) {
                        return
                    }

                    val composeFunctionModule = ModuleUtil.findModuleForPsiElement(element) ?: return

                    element.valueParameters.forEach { ktParameter ->
                        val typeReference = ktParameter.typeReference ?: return@forEach

                        val ktType = (ktParameter.resolveToDescriptorIfAny() as? CallableDescriptor)?.returnType
                            ?: return@forEach

                        if (!ktType.isStable(holder.project, composeFunctionModule)) {
                            holder.registerProblem(typeReference, unstableArgumentError)
                        }
                    }
                    println("Found composable function: ${element.name}")
                }
            }
        }
    }
}


const val unstableArgumentError = "Argument class is not stable and can cause recomposition"