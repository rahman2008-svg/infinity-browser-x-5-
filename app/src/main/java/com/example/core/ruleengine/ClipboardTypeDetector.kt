package com.example.core.ruleengine

import java.util.regex.Pattern

enum class ClipboardType {
    URL, EMAIL, PHONE, TEXT
}

data class ClipboardSuggestion(
    val text: String,
    val type: ClipboardType,
    val actionLabel: String,
    val actionUrl: String
)

/**
 * ClipboardTypeDetector.kt
 * 
 * Framework-independent regex-based detector যা ক্লিপবোর্ডের টেক্সট বিশ্লেষণ করে 
 * URL, Email, Phone বা সাধারণ টেক্সট সনাক্ত করে স্মার্ট সাজেশন তৈরি করে।
 */
object ClipboardTypeDetector {

    private val URL_PATTERN = Pattern.compile(
        "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?\$",
        Pattern.CASE_INSENSITIVE
    )
    
    private val EMAIL_PATTERN = Pattern.compile(
        "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}\$",
        Pattern.CASE_INSENSITIVE
    )
    
    private val PHONE_PATTERN = Pattern.compile(
        "^(\\+?\\d{1,3}[- .]?)?\\(?\\d{3}\\)?[- .]?\\d{3}[- .]?\\d{4}\$"
    )

    fun detect(rawText: String?): ClipboardSuggestion? {
        if (rawText.isNullOrBlank()) return null
        val trimmed = rawText.trim()
        if (trimmed.length > 500) return null // Too long for a quick suggestion

        return when {
            isUrl(trimmed) -> {
                val validUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                    trimmed
                } else {
                    "https://$trimmed"
                }
                ClipboardSuggestion(
                    text = trimmed,
                    type = ClipboardType.URL,
                    actionLabel = "Open Link",
                    actionUrl = validUrl
                )
            }
            EMAIL_PATTERN.matcher(trimmed).matches() -> {
                ClipboardSuggestion(
                    text = trimmed,
                    type = ClipboardType.EMAIL,
                    actionLabel = "Send Email",
                    actionUrl = "mailto:$trimmed"
                )
            }
            PHONE_PATTERN.matcher(trimmed).matches() && trimmed.count { it.isDigit() } >= 7 -> {
                ClipboardSuggestion(
                    text = trimmed,
                    type = ClipboardType.PHONE,
                    actionLabel = "Call Number",
                    actionUrl = "tel:$trimmed"
                )
            }
            trimmed.length >= 3 -> {
                ClipboardSuggestion(
                    text = trimmed,
                    type = ClipboardType.TEXT,
                    actionLabel = "Search Web",
                    actionUrl = trimmed // Will be passed to search engine
                )
            }
            else -> null
        }
    }

    private fun isUrl(text: String): Boolean {
        if (text.startsWith("http://") || text.startsWith("https://") || text.startsWith("www.")) {
            return true
        }
        return URL_PATTERN.matcher(text).matches() && text.contains(".") && !text.contains(" ")
    }
}
