package com.example.core.ruleengine

import java.net.URLEncoder

/**
 * SearchResolver.kt
 * 
 * কোর রুল ইঞ্জিন: টাইপ করা ইনপুট URL, শর্টকাট অ্যালিয়াস, নাকি সাধারণ সার্চ কোয়েরি তা নিরূপণ করে।
 * এটি সম্পূর্ণ Framework-Independent (কোনো Android import ছাড়া), ফলে খুব সহজেই ইউনিট টেস্ট করা সম্ভব।
 */

enum class SearchEngine(val displayName: String, val searchUrlTemplate: String) {
    GOOGLE("Google", "https://www.google.com/search?q=%s"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s"),
    BING("Bing", "https://www.bing.com/search?q=%s"),
    BRAVE("Brave Search", "https://search.brave.com/search?q=%s");

    fun formatQuery(query: String): String {
        val encoded = try {
            URLEncoder.encode(query, "UTF-8")
        } catch (e: Exception) {
            query.replace(" ", "+")
        }
        return searchUrlTemplate.format(encoded)
    }
}

sealed interface ResolveResult {
    val targetUrl: String
    val displayLabel: String

    data class DirectUrl(override val targetUrl: String) : ResolveResult {
        override val displayLabel: String get() = targetUrl
    }

    data class ShortcutUrl(val alias: String, override val targetUrl: String) : ResolveResult {
        override val displayLabel: String get() = "$alias → $targetUrl"
    }

    data class SearchQuery(val query: String, val engine: SearchEngine, override val targetUrl: String) : ResolveResult {
        override val displayLabel: String get() = "$query (${engine.displayName})"
    }
}

object SearchResolver {

    // শর্টকাট অ্যালিয়াস লুকআপ টেবিল (O(1) lookup)
    private val SHORTCUT_ALIASES = mapOf(
        "yt" to "https://www.youtube.com",
        "youtube" to "https://www.youtube.com",
        "fb" to "https://www.facebook.com",
        "facebook" to "https://www.facebook.com",
        "gh" to "https://github.com",
        "github" to "https://github.com",
        "rd" to "https://www.reddit.com",
        "reddit" to "https://www.reddit.com",
        "w" to "https://www.wikipedia.org",
        "wiki" to "https://www.wikipedia.org",
        "g" to "https://www.google.com",
        "google" to "https://www.google.com",
        "ddg" to "https://duckduckgo.com",
        "duck" to "https://duckduckgo.com",
        "amz" to "https://www.amazon.com",
        "amazon" to "https://www.amazon.com",
        "x" to "https://x.com",
        "tw" to "https://x.com",
        "map" to "https://maps.google.com",
        "maps" to "https://maps.google.com",
        "ai" to "https://aistudio.google.com",
        "gpt" to "https://chatgpt.com"
    )

    // পরিচিত TLDs (Top Level Domains) চেনার জন্য সেট
    private val KNOWN_TLDS = setOf(
        ".com", ".org", ".net", ".edu", ".gov", ".io", ".co", ".app", ".dev",
        ".ai", ".in", ".uk", ".de", ".jp", ".ca", ".au", ".ru", ".ch", ".it",
        ".nl", ".se", ".no", ".es", ".fr", ".xyz", ".me", ".tv", ".bd", ".info"
    )

    // IP address pattern (e.g., 192.168.1.1, localhost:8080)
    private val IP_OR_LOCAL_REGEX = Regex("^localhost(:\\d+)?$|^(\\d{1,3}\\.){3}\\d{1,3}(:\\d+)?$")

    /**
     * ইনপুট স্ট্রিং বিশ্লেষণ করে সঠিক ResolveResult প্রদান করে।
     */
    fun resolve(input: String, defaultEngine: SearchEngine = SearchEngine.GOOGLE): ResolveResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ResolveResult.DirectUrl("https://www.google.com")
        }

        val lower = trimmed.lowercase()

        // ১. শর্টকাট চেক (যেমন "yt", "fb")
        SHORTCUT_ALIASES[lower]?.let { shortcutUrl ->
            return ResolveResult.ShortcutUrl(alias = lower, targetUrl = shortcutUrl)
        }

        // ২. সরাসরি URL প্রোটোকল চেক (http://, https://, ftp://, file://)
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("ftp://") || lower.startsWith("file://")) {
            return ResolveResult.DirectUrl(trimmed)
        }

        // ৩. স্পেস থাকলে সরাসরি সার্চ কোয়েরি
        if (trimmed.contains(" ")) {
            return ResolveResult.SearchQuery(
                query = trimmed,
                engine = defaultEngine,
                targetUrl = defaultEngine.formatQuery(trimmed)
            )
        }

        // ৪. Localhost বা IP Address চেক
        if (IP_OR_LOCAL_REGEX.matches(lower)) {
            return ResolveResult.DirectUrl("http://$trimmed")
        }

        // ৫. ডোমেইন TLD চেক (যেমন example.com, test.io, my-site.dev:3000)
        val domainWithoutPort = lower.substringBefore("/")
        val hasTld = KNOWN_TLDS.any { tld ->
            domainWithoutPort.endsWith(tld) || domainWithoutPort.contains("$tld:")
        }

        if (hasTld) {
            return ResolveResult.DirectUrl("https://$trimmed")
        }

        // ৬. সাধারণ শব্দ (যেটিতে প্রোটোকল বা TLD নেই) হলে ডিফল্ট সার্চ ইঞ্জিনে কোয়েরি
        return ResolveResult.SearchQuery(
            query = trimmed,
            engine = defaultEngine,
            targetUrl = defaultEngine.formatQuery(trimmed)
        )
    }

    /**
     * কোনো URL থেকে প্রধান ডোমেইন বা হোস্টনেম বের করার ইউটিলিটি।
     */
    fun extractDomain(url: String): String {
        return try {
            var clean = url.trim()
            if (clean.startsWith("http://", ignoreCase = true)) clean = clean.substring(7)
            if (clean.startsWith("https://", ignoreCase = true)) clean = clean.substring(8)
            clean = clean.substringBefore("/").substringBefore("?")
            if (clean.startsWith("www.", ignoreCase = true)) clean = clean.substring(4)
            clean.ifEmpty { url }
        } catch (e: Exception) {
            url
        }
    }
}
