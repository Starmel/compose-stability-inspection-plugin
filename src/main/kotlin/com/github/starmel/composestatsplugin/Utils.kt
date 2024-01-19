package com.github.starmel.composestatsplugin

import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.findParentOfType
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlinx.serialization.compiler.resolve.toClassDescriptor
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun PsiElement.isComposableFunction(): Boolean {

    contract {
        returns(true) implies (this@isComposableFunction is KtNamedFunction)
    }

    return this is KtNamedFunction && this.annotationEntries.any { it.shortName?.identifier == "Composable" }
}

fun KtClass.isUsedAsComposeMethodArgument(): Boolean {
    return ReferencesSearch.search(this).any { reference ->

        val namedFunction = reference.element.findParentOfType<KtNamedFunction>()
        if (namedFunction != null && namedFunction.isComposableFunction()) {
            return@any true
        }

        val usedInKtClass = reference.element.findParentOfType<KtClass>()
        usedInKtClass != null && (usedInKtClass.isUsedAsComposeMethodArgument())
    }
}


fun KtClass.hasStableComposeAnnotation(): Boolean {
    return findAnnotation(FqName("androidx.compose.runtime.Stable")) != null ||
            findAnnotation(FqName("androidx.compose.runtime.Immutable")) != null
}

fun KotlinType.hasStableComposeAnnotation(): Boolean {
    return toClassDescriptor?.annotations?.any {
        val fqName = it.fqName?.asString()
        fqName == "androidx.compose.runtime.Stable" ||
                fqName?.endsWith("Immutable") == true // To cover custom KMM type-aliase annotations.
    } == true
}