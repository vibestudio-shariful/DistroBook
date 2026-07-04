package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.viewmodel.AppViewModel

@Composable
fun t(viewModel: AppViewModel, bn: String, en: String): String {
    val isEnglish by viewModel.isEnglish.collectAsState()
    return if (isEnglish) en else bn
}

fun tNonCompose(isEnglish: Boolean, bn: String, en: String): String {
    return if (isEnglish) en else bn
}
