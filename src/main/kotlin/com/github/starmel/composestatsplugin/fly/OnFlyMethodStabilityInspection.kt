package com.github.starmel.composestatsplugin.fly

import ComposeStatsBundle
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.backend.jvm.ir.psiElement
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isEnum
import org.jetbrains.kotlin.types.typeUtil.isInterface
import org.jetbrains.kotlinx.serialization.compiler.resolve.toClassDescriptor
import org.jetbrains.kotlin.psi.namedFunctionVisitor

class OnFlyMethodStabilityInspection : AbstractKotlinInspection() {

    override fun getDisplayName(): String {
        return "Unstable type used as Compose method argument"
    }

    override fun getStaticDescription(): String {
        return "Unstable type used as Compose method argument"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        namedFunctionVisitor { namedFunction ->
            val hasComposableAnnotation =
                namedFunction.findAnnotation(FqName("androidx.compose.runtime.Composable")) != null

            if (!hasComposableAnnotation) {
                return@namedFunctionVisitor
            }

            val composeFunctionModule = ModuleUtil.findModuleForPsiElement(namedFunction) ?: return@namedFunctionVisitor

            namedFunction.valueParameters.forEach { ktParameter ->
                val typeReference = ktParameter.typeReference ?: return@forEach

                val ktType = (ktParameter.resolveToDescriptorIfAny() as? CallableDescriptor)?.returnType
                    ?: return@forEach

                val instabilityCause = ktType.getInstabilityCause(composeFunctionModule)
                if (instabilityCause != null) {
                    holder.registerProblem(typeReference, ComposeStatsBundle.message("compose.unstable", instabilityCause))
                }
            }
            println(ComposeStatsBundle.message("found.composable.function", namedFunction.name.orEmpty()))
        }
}


fun KotlinType.hasStableComposeAnnotation(): Boolean {
    return toClassDescriptor?.annotations?.any {
        val fqName = it.fqName?.asString()
        fqName == "androidx.compose.runtime.Stable" ||
                fqName?.endsWith("Immutable") == true // To cover custom KMM type-aliase annotations.
    } == true
}


fun KotlinType.isImmutableTypeLibrary(): Boolean {
    return this.fqName?.asString()?.startsWith("kotlinx.collections.immutable") == true
}

fun KotlinType.isStandardLibrary(): Boolean {
    // toClassDescriptor() to handle type aliases
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

fun KotlinType.getInstabilityCause(composeFunctionModule: Module): String? {
    val shortName = fqName?.shortName()?.asString()

    if (isEnum() || hasStableComposeAnnotation() || isImmutableTypeLibrary() || isStandardLibrary()) {
        return null
    }
    if (isFunctionTypeOrSubtype) {
        val projectionList = getValueParameterTypesFromFunctionType()
            .takeIf { it.isNotEmpty() } ?: return null

        projectionList.forEach { typeProjection ->
            val instabilityCause = typeProjection.type.getInstabilityCause(composeFunctionModule)
            if (instabilityCause != null) {
                return instabilityCause
            }
        }

        return null
    } else {
        if (isInterface()) {
            return ComposeStatsBundle.message("cause.is.interface", shortName.toString())
        }

        memberScope.getContributedDescriptors().filterIsInstance<PropertyDescriptorImpl>()
            .forEach { descriptor ->
                    if (descriptor.isVar) {
                        return ComposeStatsBundle.message("cause.contains.mutable.property", shortName.toString(), descriptor.name)
                    } else {
                        val propKtClass = descriptor.type

                        val instabilityCause = propKtClass.getInstabilityCause(composeFunctionModule)
                        if (instabilityCause != null) {
                            return ComposeStatsBundle.message("cause.contains.unstable.property", shortName.toString(), descriptor.name, instabilityCause)
                    }
                }
            }
    }

    // Check same module

    val psiElement = constructor.declarationDescriptor?.psiElement ?: return "$shortName is cannot be resolved (#1)"
    val typeModule = ModuleUtil.findModuleForPsiElement(psiElement) ?: return "$shortName is cannot be resolved (#2)"

    if (typeModule != composeFunctionModule) {
        return ComposeStatsBundle.message("cause.is.not.same.module", shortName.toString())
    }

    return null
}