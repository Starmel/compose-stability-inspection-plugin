package com.github.starmel.composestatsplugin

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.ComposeStatsBundle"

object ComposeStatsBundle: DynamicBundle(BUNDLE) {

    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = getMessage(key, *params)
}