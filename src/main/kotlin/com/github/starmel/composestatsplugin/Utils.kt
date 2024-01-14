package com.github.starmel.composestatsplugin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun PsiElement.isComposableFunction(): Boolean {

    contract {
        returns(true) implies (this@isComposableFunction is KtNamedFunction)
    }

    return this is KtNamedFunction && this.annotationEntries.any { it.shortName?.identifier == "Composable" }
}
