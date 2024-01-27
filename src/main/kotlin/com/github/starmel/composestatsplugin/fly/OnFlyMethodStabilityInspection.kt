package com.github.starmel.composestatsplugin.fly

import com.github.starmel.composestatsplugin.ComposeStatsBundle
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.backend.jvm.ir.psiElement
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.isSealed
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.namedFunctionVisitor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isEnum
import org.jetbrains.kotlinx.serialization.compiler.resolve.toClassDescriptor

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
                    holder.registerProblem(
                        typeReference,
                        ComposeStatsBundle.message("compose.unstable", instabilityCause)
                    )
                }
            }
        }
}


fun ClassDescriptor.hasStableComposeAnnotation(): Boolean {
    return annotations.any {
        val fqName = it.fqName?.asString()
        fqName == "androidx.compose.runtime.Stable" ||
                fqName?.endsWith("Immutable") == true // To cover custom KMM type-aliase annotations.
    }
}

fun ClassDescriptor.hasStabilityInferredAnnotation(): Boolean {
    return annotations.any {
        val fqName = it.fqName?.asString()
        fqName == "androidx.compose.runtime.internal.StabilityInferred"
    }
}

fun KotlinType.isImmutableTypeLibrary(): Boolean {
    return this.fqName?.asString()?.startsWith("kotlinx.collections.immutable") == true
}

fun ClassDescriptor.isParentStableSealedClass(): Boolean {
    return parents.firstOrNull()
        ?.takeIf { it.isSealed() && it.hasStableComposeAnnotation() } != null
}

fun ClassDescriptor.isStandardLibrary(): Boolean {
    return fqNameOrNull()?.asString()?.let {
        setOf(
            "kotlin.Boolean",
            "kotlin.Byte",
            "kotlin.Char",
            "kotlin.Double",
            "kotlin.Float",
            "kotlin.Int",
            "kotlin.Long",
            "kotlin.Short",
            "kotlin.String",
        ).contains(it)
    } ?: false
}

fun KotlinType.getInstabilityCause(
    composeFunctionModule: Module,
    resolveParent: MutableSet<FqName> = mutableSetOf()
): String? {
    val shortName = fqName?.shortName()?.asString()

    // dummy recursion break
    if (resolveParent.contains(fqName)) {
        return null
    } else {
        resolveParent.add(fqName ?: return null)
    }

    val classDescriptor = toClassDescriptor ?: return "$shortName cannot be resolved (#3)"

    if (classDescriptor.source is JavaSourceElement) {
        return "$shortName unstable as it is Java class"
    }

    if (isEnum()
        || classDescriptor.hasStableComposeAnnotation()
        || isImmutableTypeLibrary()
        || classDescriptor.isStandardLibrary()
        || classDescriptor.isParentStableSealedClass()
    ) {
        return null
    }

    val isListType = fqName?.asString()?.startsWith("kotlin.collections.List") == true

    if (isListType) {
        return "instead of List use [kotlinx.collections.immutable](https://github.com/Kotlin/kotlinx.collections.immutable) library"
    }

    val isTupleType = fqName?.asString()?.startsWith("kotlin.Pair") == true ||
            fqName?.asString()?.startsWith("kotlin.Triple") == true

    if (isTupleType) {
        arguments.forEach { typeProjection ->
            val instabilityCause = typeProjection.type.getInstabilityCause(composeFunctionModule, resolveParent)
            if (instabilityCause != null) {
                return instabilityCause
            }
        }
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

        memberScope.getContributedDescriptors().filterIsInstance<PropertyDescriptorImpl>()
            .forEach { descriptor ->
                if (descriptor.isVar) {
                    return ComposeStatsBundle.message(
                        "cause.contains.mutable.property",
                        shortName.toString(),
                        descriptor.name
                    )
                } else {
                    val propKtClass = descriptor.type

                    val instabilityCause =
                        propKtClass.getInstabilityCause(composeFunctionModule, resolveParent)
                    if (instabilityCause != null) {
                        return ComposeStatsBundle.message(
                            "cause.contains.unstable.property",
                            shortName.toString(),
                            descriptor.name,
                            instabilityCause
                        )
                    }
                }
            }
    }

    if (classDescriptor.source == SourceElement.NO_SOURCE) {
        return "$shortName has no source to validate"
    }

    if (classDescriptor.hasStabilityInferredAnnotation()) {
        return "stability will be inferred at runtime"
    }

    // Check same module

    val isSameModule: Boolean

    val isLibraryCode = classDescriptor.source is KotlinJvmBinarySourceElement
    if (isLibraryCode) {
        isSameModule = false
    } else {
        val psiElement = classDescriptor.psiElement ?: return "$shortName cannot be resolved (#1)"
        val typeModule = ModuleUtil.findModuleForPsiElement(psiElement) ?: return "$shortName cannot be resolved (#2)"
        isSameModule = typeModule == composeFunctionModule
    }

    if (!isSameModule) {
        return ComposeStatsBundle.message("cause.is.not.same.module", shortName.toString())
    }

    return null
}