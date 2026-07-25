package com.example.core.ruleengine

import android.webkit.CookieManager
import android.webkit.WebStorage
import com.example.core.browser.TabSessionManager
import com.example.core.browser.WebViewController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FireButtonEngine.kt
 * 
 * ব্রাউজারের ওয়ান-ট্যাপ প্রাইভেসী ক্লিনআপ ইঞ্জিন। সক্রিয় সেশনের সমস্ত কুকি, ক্যাশ, 
 * ওয়েব স্টোরেজ এবং সক্রিয় ট্যাব মুহূর্তের মধ্যে মুছে ফেলে এবং হিস্টরি/বুকমার্ক অক্ষত রাখে।
 */
object FireButtonEngine {

    suspend fun clearAllSessionData(
        webViewController: WebViewController?,
        tabSessionManager: TabSessionManager
    ) = withContext(Dispatchers.Main) {
        try {
            // 1. Destroy active WebView instances and clear memory/disk caches
            webViewController?.clearSessionData()

            // 2. Clear cookies
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.removeSessionCookies(null)
            cookieManager.flush()

            // 3. Clear HTML5 Web Storage (localStorage, sessionStorage, indexedDB)
            WebStorage.getInstance().deleteAllData()

            // 4. Close all tabs in memory and open a clean new tab
            tabSessionManager.closeAllTabs()
            tabSessionManager.openNewTab(
                url = "about:blank",
                title = "New Tab",
                isIncognito = false,
                spaceCategory = "General"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
