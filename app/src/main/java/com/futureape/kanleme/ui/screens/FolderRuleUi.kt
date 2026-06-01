package com.futureape.kanleme.ui.screens

internal fun normalizeFolderRuleForUi(path: String): String {
    val cleaned = path.trim().replace('\\', '/').trim('/')
    return if (cleaned.isBlank()) "" else cleaned + "/"
}

internal fun folderRuleMatchesForUi(folderPath: String, rules: Set<String>): Boolean {
    val normalizedPath = normalizeFolderRuleForUi(folderPath)
    if (normalizedPath.isBlank() || rules.isEmpty()) return false
    val folderName = normalizedPath.trim('/').substringAfterLast('/')
    return rules.any { rule ->
        val normalizedRule = normalizeFolderRuleForUi(rule)
        if (normalizedRule.isBlank()) {
            false
        } else {
            val ruleBody = normalizedRule.trim('/')
            val ruleName = ruleBody.substringAfterLast('/')
            normalizedPath == normalizedRule ||
                normalizedPath.startsWith(normalizedRule) ||
                normalizedPath.endsWith("/" + ruleBody + "/") ||
                folderName.equals(ruleBody, ignoreCase = true) ||
                folderName.equals(ruleName, ignoreCase = true)
        }
    }
}
