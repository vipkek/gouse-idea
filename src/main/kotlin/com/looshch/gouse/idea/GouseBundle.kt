package com.looshch.gouse.idea

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE_NAME = "messages.GouseBundle"

object GouseBundle : DynamicBundle(BUNDLE_NAME) {
    fun message(@PropertyKey(resourceBundle = BUNDLE_NAME) key: String, vararg params: Any): String =
        getMessage(key, *params)
}
