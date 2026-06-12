package com.futureape.kanleme.ui.i18n

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

sealed interface UiText {
    data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Plural(@PluralsRes val id: Int, val quantity: Int, val args: List<Any> = listOf(quantity)) : UiText
    data class Dynamic(val value: String) : UiText

    fun asString(context: Context): String = when (this) {
        is Resource -> context.getString(id, *args.toTypedArray())
        is Plural -> context.resources.getQuantityString(id, quantity, *args.toTypedArray())
        is Dynamic -> value
    }
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Resource -> stringResource(id, *args.toTypedArray())
    is UiText.Plural -> pluralStringResource(id, quantity, *args.toTypedArray())
    is UiText.Dynamic -> value
}

fun uiText(@StringRes id: Int, vararg args: Any): UiText = UiText.Resource(id, args.toList())

fun dynamicUiText(value: String): UiText = UiText.Dynamic(value)
