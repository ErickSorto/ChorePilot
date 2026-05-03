package com.sortoapps.chorepilot.core.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

sealed class StringValue {
    data class DynamicString(val value: String) : StringValue()

    class Resource(
        private val resId: StringResource,
        vararg args: Any,
    ) : StringValue() {
        private val formatArgs = args

        @Composable
        fun resolve(): String = stringResource(resId, *formatArgs)
    }

    data object Empty : StringValue()

    @Composable
    fun asString(): String = when (this) {
        Empty -> ""
        is DynamicString -> value
        is Resource -> resolve()
    }
}
