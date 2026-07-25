package com.example.core.ruleengine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast

/**
 * QRActionType: স্ক্যান হওয়া কিউআর কোডের ধরন
 */
enum class QRActionType {
    URL,
    WIFI,
    TEL,
    EMAIL,
    VCARD,
    TEXT
}

/**
 * QRPayloadResult: পার্স করা কিউআর কোড ডেটা ও নেওয়ার সম্ভাব্য অ্যাকশন
 */
data class QRPayloadResult(
    val type: QRActionType,
    val rawText: String,
    val displayTitle: String,
    val displaySubtitle: String,
    val actionLabel: String
)

/**
 * QRPayloadRouter.kt
 * 
 * ML Kit Barcode স্ক্যানার থেকে প্রাপ্ত raw text পার্স করে তার টাইপ অনুযায়ী অ্যাকশন ও রাউটিং নির্ধারণ করে।
 */
object QRPayloadRouter {

    fun parse(rawText: String): QRPayloadResult {
        val trimmed = rawText.trim()
        
        // 1. WiFi
        if (trimmed.startsWith("WIFI:", ignoreCase = true)) {
            val ssid = extractWifiField(trimmed, "S:")
            val pass = extractWifiField(trimmed, "P:")
            val auth = extractWifiField(trimmed, "T:")
            return QRPayloadResult(
                type = QRActionType.WIFI,
                rawText = trimmed,
                displayTitle = "Wi-Fi Network: ${ssid.ifEmpty { "Unknown" }}",
                displaySubtitle = if (pass.isNotEmpty()) "Security: ${auth.ifEmpty { "WPA" }} • Password hidden" else "Open Network",
                actionLabel = "Connect / Copy Password"
            )
        }

        // 2. TEL
        if (trimmed.startsWith("tel:", ignoreCase = true) || trimmed.startsWith("TEL:") || trimmed.matches(Regex("^(\\+\\d{1,3}[- ]?)?\\d{7,12}\$"))) {
            val number = trimmed.removePrefix("tel:").removePrefix("TEL:").trim()
            return QRPayloadResult(
                type = QRActionType.TEL,
                rawText = trimmed,
                displayTitle = "Phone Number",
                displaySubtitle = number,
                actionLabel = "Call Number"
            )
        }

        // 3. EMAIL
        if (trimmed.startsWith("mailto:", ignoreCase = true) || trimmed.startsWith("MATMSG:", ignoreCase = true) || trimmed.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)\$"))) {
            val email = trimmed.removePrefix("mailto:").removePrefix("MATMSG:TO:").substringBefore(";").trim()
            return QRPayloadResult(
                type = QRActionType.EMAIL,
                rawText = trimmed,
                displayTitle = "Email Address",
                displaySubtitle = email,
                actionLabel = "Send Email"
            )
        }

        // 4. VCARD
        if (trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) || trimmed.startsWith("BEGIN:MECARD", ignoreCase = true)) {
            val name = extractVCardName(trimmed)
            val phone = extractVCardPhone(trimmed)
            return QRPayloadResult(
                type = QRActionType.VCARD,
                rawText = trimmed,
                displayTitle = "Contact Card: ${name.ifEmpty { "New Contact" }}",
                displaySubtitle = if (phone.isNotEmpty()) "Phone: $phone" else "vCard Contact Data",
                actionLabel = "Add to Contacts"
            )
        }

        // 5. URL
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) || trimmed.matches(Regex("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}(/.*)?\$"))) {
            val url = if (trimmed.startsWith("http")) trimmed else "https://$trimmed"
            return QRPayloadResult(
                type = QRActionType.URL,
                rawText = url,
                displayTitle = "Web Link",
                displaySubtitle = url,
                actionLabel = "Open in Browser"
            )
        }

        // 6. TEXT (Fallback)
        return QRPayloadResult(
            type = QRActionType.TEXT,
            rawText = trimmed,
            displayTitle = "Plain Text / Code",
            displaySubtitle = if (trimmed.length > 80) trimmed.take(80) + "..." else trimmed,
            actionLabel = "Search in Browser / Copy"
        )
    }

    /**
     * পার্স করা রেজাল্ট অনুযায়ী সরাসরি সিস্টেম বা ব্রাউজার অ্যাকশন ট্রিগার করে।
     */
    fun executeAction(context: Context, result: QRPayloadResult, onOpenInBrowser: (String) -> Unit, onCopyText: (String) -> Unit) {
        try {
            when (result.type) {
                QRActionType.URL -> {
                    onOpenInBrowser(result.rawText)
                }
                QRActionType.TEL -> {
                    val number = result.rawText.removePrefix("tel:").removePrefix("TEL:").trim()
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                QRActionType.EMAIL -> {
                    val email = result.rawText.removePrefix("mailto:").removePrefix("MATMSG:TO:").substringBefore(";").trim()
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                QRActionType.VCARD -> {
                    val name = extractVCardName(result.rawText)
                    val phone = extractVCardPhone(result.rawText)
                    val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                        type = ContactsContract.RawContacts.CONTENT_TYPE
                        if (name.isNotEmpty()) putExtra(ContactsContract.Intents.Insert.NAME, name)
                        if (phone.isNotEmpty()) putExtra(ContactsContract.Intents.Insert.PHONE, phone)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                QRActionType.WIFI -> {
                    val pass = extractWifiField(result.rawText, "P:")
                    if (pass.isNotEmpty()) {
                        onCopyText(pass)
                        Toast.makeText(context, "Wi-Fi Password copied to clipboard!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Open Wi-Fi network: ${extractWifiField(result.rawText, "S:")}", Toast.LENGTH_LONG).show()
                    }
                }
                QRActionType.TEXT -> {
                    onOpenInBrowser(result.rawText)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not perform action: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractWifiField(raw: String, tag: String): String {
        val index = raw.indexOf(tag, ignoreCase = true)
        if (index == -1) return ""
        val start = index + tag.length
        val end = raw.indexOf(";", start)
        return if (end != -1) raw.substring(start, end) else raw.substring(start)
    }

    private fun extractVCardName(raw: String): String {
        val lines = raw.lines()
        for (line in lines) {
            if (line.startsWith("FN:", ignoreCase = true) || line.startsWith("N:", ignoreCase = true)) {
                return line.substringAfter(":").replace(";", " ").trim()
            }
        }
        return ""
    }

    private fun extractVCardPhone(raw: String): String {
        val lines = raw.lines()
        for (line in lines) {
            if (line.startsWith("TEL", ignoreCase = true)) {
                return line.substringAfter(":").trim()
            }
        }
        return ""
    }
}
