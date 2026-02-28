package com.ucw.beatu.shared.common.threading

object TaskNames {
    private const val TOKEN_LIMIT = 20

    fun of(feature: String, action: String, vararg dims: Pair<String, Any?>): String {
        val prefix = "th.task.${normalizeToken(feature)}.${normalizeToken(action)}"
        if (dims.isEmpty()) return prefix
        val normalizedDims = dims
            .filter { it.first.isNotBlank() }
            .joinToString(".") { (key, value) ->
                "${normalizeToken(key)}-${normalizeValue(value)}"
            }
        return "$prefix.$normalizedDims"
    }

    fun fromLegacy(rawName: String): String {
        val raw = rawName.trim()
        if (raw.isEmpty()) return of(feature = "legacy", action = "unknown")
        if (raw.startsWith("th.task.")) return raw

        val normalized = raw
            .replace(':', '.')
            .replace('/', '.')
            .replace(Regex("\\s+"), "_")
            .replace(Regex("[^A-Za-z0-9._\\-]"), "-")
            .trim('.')
        val head = normalized.split('.').filter { it.isNotBlank() }
        val feature = head.firstOrNull() ?: "legacy"
        val action = if (head.size > 1) head.drop(1).joinToString("_") else "run"
        return of(
            feature = "legacy_${feature}",
            action = action,
            "raw" to raw
        )
    }

    private fun normalizeValue(value: Any?): String {
        if (value == null) return "na"
        val raw = value.toString().trim()
        if (raw.isEmpty()) return "empty"
        val normalized = raw
            .replace(Regex("\\s+"), "_")
            .replace(Regex("[^A-Za-z0-9_\\-]"), "-")
            .trim('-')
            .lowercase()
        val clipped = if (normalized.length > TOKEN_LIMIT) normalized.take(TOKEN_LIMIT) else normalized
        return "$clipped~${raw.hashCode().toUInt().toString(16)}"
    }

    private fun normalizeToken(value: String): String {
        val normalized = value
            .trim()
            .replace(Regex("\\s+"), "_")
            .replace(Regex("[^A-Za-z0-9_\\-]"), "-")
            .trim('-')
            .lowercase()
        if (normalized.isEmpty()) return "unknown"
        return if (normalized.length > TOKEN_LIMIT) normalized.take(TOKEN_LIMIT) else normalized
    }
}
