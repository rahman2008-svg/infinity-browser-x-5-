package com.example.core.ruleengine

sealed class Command(val trigger: String, val title: String, val description: String, val iconName: String) {
    object OpenDownloads : Command(">downloads", "Downloads", "View downloaded files and progress", "download")
    object OpenHistory : Command(">history", "History", "Browse past visited pages", "history")
    object OpenBookmarks : Command(">bookmarks", "Bookmarks", "Open saved bookmarks and folders", "bookmark")
    object OpenSettings : Command(">settings", "Settings", "Configure preferences, privacy, and theme", "settings")
    object NewTab : Command(">new tab", "New Tab", "Open a fresh blank tab", "add")
    object ClearCache : Command(">clear cache", "Fire Button (Clear Data)", "Wipe active cookies, cache, and session tabs", "local_fire_department")
    object NewIncognitoTab : Command(">incognito", "New Incognito Tab", "Open a private browsing tab without history", "privacy_tip")
    object OpenNotes : Command(">notes", "Notes", "View integrated notes and reminders", "note")
    object OpenReadingList : Command(">reading list", "Reading List", "View offline saved reading articles", "chrome_reader_mode")

    companion object {
        val ALL_COMMANDS = listOf(
            OpenDownloads, OpenHistory, OpenBookmarks, OpenSettings,
            NewTab, ClearCache, NewIncognitoTab, OpenNotes, OpenReadingList
        )
    }
}

/**
 * CommandParser.kt
 * 
 * Framework-independent command palette parser। Address bar-এ `>` দিয়ে শুরু করলে 
 * দ্রুত অ্যাকশন ও নেভিগেশনের জন্য কমান্ড সাজেস্ট ও পার্স করে।
 */
object CommandParser {

    fun isCommandMode(query: String): Boolean {
        return query.trimStart().startsWith(">")
    }

    fun getSuggestions(query: String): List<Command> {
        val trimmed = query.trimStart().lowercase()
        if (!trimmed.startsWith(">")) return emptyList()
        
        val prefix = trimmed
        return Command.ALL_COMMANDS.filter { cmd ->
            cmd.trigger.startsWith(prefix) || cmd.title.lowercase().contains(prefix.removePrefix(">").trim())
        }
    }

    fun parseExactCommand(query: String): Command? {
        val trimmed = query.trim().lowercase()
        return Command.ALL_COMMANDS.find { it.trigger == trimmed }
    }
}
