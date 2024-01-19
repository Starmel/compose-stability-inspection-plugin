package com.github.starmel.composestatsplugin.fly

import com.github.starmel.composestatsplugin.hasStableComposeAnnotation
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.backend.jvm.ir.psiElement
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isEnum
import org.jetbrains.kotlin.types.typeUtil.isInterface

class OnFlyClassStabilityInspection : AbstractKotlinInspection() {

    override fun getDisplayName(): String {
        return "Unstable type used as property."
    }

    override fun getStaticDescription(): String? {
        return "Unstable type used as property."
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {

        return object : PsiElementVisitor() {

            override fun visitElement(element: PsiElement) {
                super.visitElement(element)

//                if (element is KtClass && !element.isEnum() && element !is KtEnumEntry) {
//
//                    val shouldCheck = element.isUsedAsComposeMethodArgument()
//
//                    if (shouldCheck && !element.hasStableComposeAnnotation()) {
//
//                        element.primaryConstructor?.valueParameters?.forEach { valueParameter ->
//                            val idElement = valueParameter.identifyingElement ?: return@forEach
//
//                            if (valueParameter.isMutable) {
//                                holder.registerProblem(idElement, mutablePropertyError)
//                            } else {
//                                val typeElement = valueParameter.typeReference ?: return@forEach
//
//                                val kotlinType = valueParameter.descriptor?.type ?: return@forEach
//
//                                if (!kotlinType.isStable(holder.project)) {
//                                    holder.registerProblem(typeElement, unstableClassAsPropertyError)
//                                }
//                            }
//                        }
//
//                        element.getProperties().forEach { property ->
//                            val descriptor = property.descriptor as? PropertyDescriptorImpl ?: return@forEach
//                            val idElement = property.identifyingElement ?: return@forEach
//
//                            if (descriptor.isVar) {
//                                holder.registerProblem(idElement, mutablePropertyError)
//                            } else {
//                                val type = descriptor.type
//                                val typeElement = property.typeReference
//                                    ?: property.childrenOfType<KtCallExpression>().takeIf { it.isNotEmpty() }?.first()
//                                    ?: return@forEach
//
//                                if (!type.isStable(holder.project)) {
//                                    holder.registerProblem(typeElement, unstableClassAsPropertyError)
//                                }
//                            }
//                        }
//                    }
//                }
            }
        }
    }

}

const val unstableClassError = "Class is unstable which can cause inefficient recompositions."

const val unstableClassAsPropertyError =
    "Property class is unstable which can cause inefficient recompositions."
const val mutablePropertyError = "Property is mutable which can cause inefficient recompositions."

fun KotlinType.isImmutableTypeLibrary(): Boolean {
    return this.fqName?.asString()?.startsWith("kotlinx.collections.immutable") == true
}

fun KotlinType.isStandardLibrary(): Boolean {
    return this.fqName?.asString()?.let {
        setOf(
            "kotlin.Boolean",
            "kotlin.Byte",
            "kotlin.Char",
            "kotlin.Double",
            "kotlin.Float",
            "kotlin.Int",
            "kotlin.Long",
            "kotlin.Short",
            "kotlin.String"
        ).contains(it)
    } ?: false
}

fun KotlinType.isStable(project: Project, composeFunctionModule: Module): Boolean {
    if (isEnum() || hasStableComposeAnnotation() || isImmutableTypeLibrary() || isStandardLibrary()) {
        return true
    }
    if (isFunctionTypeOrSubtype) {
        val projectionList = getValueParameterTypesFromFunctionType()
            .takeIf { it.isNotEmpty() } ?: return true

        projectionList.forEach { typeProjection ->
            if (!typeProjection.type.isStable(project, composeFunctionModule)) {
                return false
            }
        }

        return true
    } else {
        if (isInterface()) {
            return false
        }

        memberScope.getContributedDescriptors()
            .forEach { descriptor ->
                if (descriptor is PropertyDescriptorImpl) {
                    if (descriptor.isVar) {
                        return false
                    } else {
                        val propKtClass = descriptor.type

                        if (!propKtClass.isStable(project, composeFunctionModule)) {
                            return false
                        }
                    }
                }
            }
    }

    // Check same module

    val psiElement = constructor.declarationDescriptor?.psiElement ?: return false
    val typeModule = ModuleUtil.findModuleForPsiElement(psiElement) ?: return false

    if (typeModule != composeFunctionModule) {
        return false
    }

    return true
}