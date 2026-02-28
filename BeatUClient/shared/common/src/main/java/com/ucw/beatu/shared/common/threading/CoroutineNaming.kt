package com.ucw.beatu.shared.common.threading

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val invalidNameCharsRegex = Regex("[^A-Za-z0-9_]")
private val duplicateUnderscoreRegex = Regex("_+")

private fun buildCoroutineName(feature: String, action: String): String {
    val normalizedFeature = feature.trim().lowercase().replace(invalidNameCharsRegex, "_")
    val normalizedAction = action.trim().replace(invalidNameCharsRegex, "_")
    return "${normalizedFeature}_${normalizedAction}"
        .replace(duplicateUnderscoreRegex, "_")
        .trim('_')
}

fun CoroutineScope.launchNamed(
    feature: String,
    action: String,
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job {
    val name = CoroutineName(buildCoroutineName(feature, action))
    return launch(context + name, start, block)
}

fun <T> Flow<T>.launchInNamed(
    scope: CoroutineScope,
    feature: String,
    action: String
): Job {
    return scope.launchNamed(feature, action) {
        collect {}
    }
}
