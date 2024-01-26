package com.github.starmel.composestatsplugin.fly

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.backend.jvm.ir.psiElement
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.isSealed
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isEnum
import org.jetbrains.kotlinx.serialization.compiler.resolve.toClassDescriptor

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

                        val instabilityCause = ktType.getInstabilityCause(composeFunctionModule)
                        if (instabilityCause != null) {
                            holder.registerProblem(typeReference, "Composable unstable: $instabilityCause")
                        }
                    }
                }
            }
        }
    }
}

fun KotlinType.hasStableComposeAnnotation(): Boolean {
    return toClassDescriptor?.annotations?.hasStableComposeAnnotation() ?: false
}

fun DeclarationDescriptor.hasStableComposeAnnotation(): Boolean {
    return annotations.hasStableComposeAnnotation()
}

fun Annotations.hasStableComposeAnnotation(): Boolean {
    return any {
        val fqName = it.fqName?.asString()
        fqName == "androidx.compose.runtime.Stable" ||
                fqName?.endsWith("Immutable") == true // To cover custom KMM type-aliase annotations.
    }
}

fun KotlinType.isImmutableTypeLibrary(): Boolean {
    return this.fqName?.asString()?.startsWith("kotlinx.collections.immutable") == true
}

fun KotlinType.isParentStableSealedClass(): Boolean {
    return toClassDescriptor?.parents?.firstOrNull()
        ?.takeIf { it.isSealed() && it.hasStableComposeAnnotation() } != null
}

fun KotlinType.isStandardLibrary(): Boolean {
    // call toClassDescriptor() to handle type aliases
    return toClassDescriptor?.fqNameOrNull()?.asString()?.let {
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

fun KotlinType.getInstabilityCause(
    composeFunctionModule: Module,
    resolveParent: MutableSet<FqName> = mutableSetOf()
): String? {
    val shortName = fqName?.shortName()?.asString()

    // dummy recursion breaker
    if (resolveParent.contains(fqName)) {
        return null
    } else {
        resolveParent.add(fqName ?: return null)
    }

    if (isEnum() || hasStableComposeAnnotation() || isImmutableTypeLibrary() || isStandardLibrary() || isParentStableSealedClass()) {
        return null
    }

    if (isFunctionTypeOrSubtype) {
        val projectionList = getValueParameterTypesFromFunctionType()
            .takeIf { it.isNotEmpty() } ?: return null

        projectionList.forEach { typeProjection ->
            val instabilityCause = typeProjection.type.getInstabilityCause(composeFunctionModule, resolveParent)
            if (instabilityCause != null) {
                return instabilityCause
            }
        }

        return null
    } else {

        memberScope.getContributedDescriptors()
            .forEach { descriptor ->
                if (descriptor is PropertyDescriptorImpl) {
                    if (descriptor.isVar) {
                        return "$shortName contains mutable property '${descriptor.name}'"
                    } else {
                        val propKtClass = descriptor.type

                        val instabilityCause =
                            propKtClass.getInstabilityCause(composeFunctionModule, resolveParent)
                        if (instabilityCause != null) {
                            return "$shortName contains unstable property '${descriptor.name}' with type '$instabilityCause'"
                        }
                    }
                }
            }
    }

    // Check same module

    val psiElement = constructor.declarationDescriptor?.psiElement ?: return "$shortName cannot be resolved (#1)"
    val typeModule = ModuleUtil.findModuleForPsiElement(psiElement) ?: return "$shortName cannot be resolved (#2)"

    if (typeModule != composeFunctionModule) {
        return "$shortName is not in the same module as the composable function"
    }

    return null
}