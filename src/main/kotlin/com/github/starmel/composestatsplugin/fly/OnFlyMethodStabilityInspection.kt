package com.github.starmel.composestatsplugin.fly

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType

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

                    element.valueParameters.forEach { ktParameter ->
                        val typeReference = ktParameter.typeReference ?: return@forEach

                        val descriptor = typeReference.analyze().get(BindingContext.TYPE, typeReference)
                            ?.constructor?.declarationDescriptor as? ClassDescriptor

                        val ktType = (descriptor?.defaultType as? KotlinType) ?: return@forEach

                        if (!ktType.isFunctionTypeOrSubtype && !ktType.isStable(holder.project)) {
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