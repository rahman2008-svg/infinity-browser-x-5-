package com.example.core.ruleengine

import android.content.Context
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URI

/**
 * FilterMatcher.kt
 * 
 * EasyList-স্টাইল host-based ব্লকলিস্ট পার্সার এবং ট্র্যাকার/অ্যাড ফিল্টার ইঞ্জিন।
 * কোনো ব্যাকএন্ড বা এক্সটার্নাল এপিআই ছাড়াই সম্পূর্ণ অফলাইনে ডোমেইন ম্যাচিং করে।
 */
object FilterMatcher {

    private val blockedDomains = HashSet<String>()
    @Volatile
    private var isLoaded = false
    @Volatile
    var isAdBlockEnabled = true // টগল স্টেট

    /**
     * Assets থেকে ডিফল্ট easylist_sample.txt লোড করে।
     */
    fun init(context: Context) {
        if (isLoaded) return
        synchronized(this) {
            if (isLoaded) return
            try {
                val inputStream = context.assets.open("easylist_sample.txt")
                loadFromInputStream(inputStream)
                isLoaded = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadFromInputStream(inputStream: InputStream) {
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val trimmed = line?.trim() ?: continue
            // Ignore comments and empty lines
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                continue
            }
            // Strip any leading `||` or trailing `^` if present from standard syntax
            var cleanDomain = trimmed.removePrefix("||").removeSuffix("^").trim().lowercase()
            if (cleanDomain.isNotEmpty()) {
                blockedDomains.add(cleanDomain)
            }
        }
        reader.close()
    }

    /**
     * সাবডোমেইন চেক সহ ইউআরএল ব্লক করার যোগ্য কিনা তা পরীক্ষা করে।
     * যেমন: sub.ads.google.com চেক করার সময় sub.ads.google.com, ads.google.com, এবং google.com ম্যাচ করবে।
     */
    fun shouldBlock(url: String): Boolean {
        if (!isAdBlockEnabled || !isLoaded || url.isBlank()) return false
        if (url.startsWith("about:") || url.startsWith("data:") || url.startsWith("file:")) return false

        val host = extractHost(url)?.lowercase() ?: return false
        return isHostBlocked(host)
    }

    fun isHostBlocked(host: String): Boolean {
        if (blockedDomains.contains(host)) return true

        // Check parent domains
        var currentHost = host
        val parts = currentHost.split(".")
        if (parts.size <= 1) return false

        // Start from subdomains and remove one part from the left sequentially
        for (i in 0 until parts.size - 1) {
            val subHost = parts.subList(i, parts.size).joinToString(".")
            if (blockedDomains.contains(subHost)) {
                return true
            }
        }
        return false
    }

    private fun extractHost(url: String): String? {
        return try {
            val uri = URI(url)
            uri.host
        } catch (e: Exception) {
            null
        }
    }

    fun getBlockedDomainsCount(): Int = blockedDomains.size
}
