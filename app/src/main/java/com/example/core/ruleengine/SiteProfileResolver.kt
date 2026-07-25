package com.example.core.ruleengine

import com.example.core.data.model.SiteProfileEntity

/**
 * SiteProfileResolver.kt
 * 
 * Framework-independent rule engine class যা নির্দিষ্ট ডোমেইনের জন্য পার-সাইট কনফিগারেশন 
 * (User-Agent, Dark Mode CSS Injection, JavaScript Status, Text Zoom) নির্ধারণ করে।
 */
object SiteProfileResolver {

    const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    const val DARK_MODE_CSS_INJECTION = """
        javascript:(function() {
            if (document.getElementById('infinity-dark-override')) return;
            var style = document.createElement('style');
            style.id = 'infinity-dark-override';
            style.innerHTML = `
                html, body {
                    background-color: #121212 !important;
                    color: #e0e0e0 !important;
                    filter: invert(1) hue-rotate(180deg) !important;
                }
                img, video, iframe, canvas, svg, [style*="background-image"] {
                    filter: invert(1) hue-rotate(180deg) !important;
                }
            `;
            if (document.head) {
                document.head.appendChild(style);
            } else {
                document.documentElement.appendChild(style);
            }
        })();
    """

    fun getEffectiveUserAgent(profile: SiteProfileEntity?, defaultUserAgent: String?): String? {
        return if (profile?.desktopMode == true) {
            DESKTOP_USER_AGENT
        } else {
            defaultUserAgent
        }
    }

    fun getDarkModeInjectionJs(profile: SiteProfileEntity?, globalDarkTheme: Boolean): String? {
        val shouldInjectDark = when (profile?.darkModeOverride) {
            true -> true
            false -> false
            null -> globalDarkTheme
        }
        return if (shouldInjectDark) DARK_MODE_CSS_INJECTION.trimIndent() else null
    }

    fun isJavaScriptEnabled(profile: SiteProfileEntity?): Boolean {
        return profile?.jsEnabled ?: true
    }

    fun getTextZoom(profile: SiteProfileEntity?): Int {
        return profile?.textZoom ?: 100
    }
}
