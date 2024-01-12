package com.github.starmel.composestatsplugin

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

//class MyAnnotator : Annotator {
//
//    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
//        if (shouldAnnotate(element)) {
//            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
//                .range(element)
//                .textAttributes(DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT)
//                .tooltip("This is a tooltip")
//                .create()
//        }
//    }
//
//    private fun shouldAnnotate(element: PsiElement): Boolean {
//
//        println("Element: $element text: ${element.text}")
//
//        return element.toString() == "FUN"
//    }
//}