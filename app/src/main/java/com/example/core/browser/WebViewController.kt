package com.example.core.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.core.data.model.SiteProfileEntity
import com.example.core.ruleengine.FilterMatcher
import com.example.core.ruleengine.SiteProfileResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.net.URI

/**
 * WebViewController.kt
 * 
 * Android System WebView (Chromium-based) এর lifecycle-aware wrapper।
 * এটি পেজ লোডিং, জুম, DOM স্টোরেজ, হিস্টোরি রেকর্ডিং, Ad-blocking, ডাউনলোড এবং অফলাইন সেভ পরিচালনা করে।
 */
class WebViewController(
    private val context: Context,
    private val tabSessionManager: TabSessionManager,
    private val onRecordHistory: ((url: String, title: String) -> Unit)? = null,
    private val onDownloadRequested: ((url: String, userAgent: String, contentDisposition: String, mimetype: String, contentLength: Long) -> Unit)? = null,
    private val onBlockedRequest: (() -> Unit)? = null,
    var getSiteProfile: (suspend (domain: String) -> SiteProfileEntity?)? = null,
    var onAccentColorExtracted: ((Int?) -> Unit)? = null,
    var isGlobalDarkTheme: Boolean = false
) {
    private var webView: WebView? = null
    private var currentTabId: String? = null
    private var defaultUserAgent: String? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        FilterMatcher.init(context)
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            val host = uri.host ?: return ""
            host.removePrefix("www.")
        } catch (e: Exception) {
            ""
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(): WebView {
        if (webView == null) {
            val wv = WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    setSupportZoom(true)
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    if (defaultUserAgent == null) {
                        defaultUserAgent = userAgentString
                    }
                }

                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                    onDownloadRequested?.invoke(url, userAgent, contentDisposition, mimetype, contentLength)
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { u ->
                            currentTabId?.let { tabId ->
                                tabSessionManager.updateTabInfo(tabId, u, view?.title ?: u)
                                tabSessionManager.updateTabProgress(tabId, 10, view?.canGoBack() ?: false, view?.canGoForward() ?: false)
                            }
                            scope.launch {
                                val domain = extractDomain(u)
                                val profile = getSiteProfile?.invoke(domain)
                                view?.settings?.apply {
                                    userAgentString = SiteProfileResolver.getEffectiveUserAgent(profile, defaultUserAgent)
                                    javaScriptEnabled = SiteProfileResolver.isJavaScriptEnabled(profile)
                                    textZoom = SiteProfileResolver.getTextZoom(profile)
                                }
                            }
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        url?.let { u ->
                            val title = view?.title ?: u
                            currentTabId?.let { tabId ->
                                tabSessionManager.updateTabInfo(tabId, u, title)
                                tabSessionManager.updateTabProgress(tabId, 100, view?.canGoBack() ?: false, view?.canGoForward() ?: false)
                            }
                            val currentTab = tabSessionManager.tabs.value.find { it.id == currentTabId }
                            if (currentTab?.isIncognito != true && !u.startsWith("about:")) {
                                onRecordHistory?.invoke(u, title)
                            }
                            scope.launch {
                                val domain = extractDomain(u)
                                val profile = getSiteProfile?.invoke(domain)
                                val js = SiteProfileResolver.getDarkModeInjectionJs(profile, isGlobalDarkTheme)
                                if (js != null && view != null) {
                                    view.evaluateJavascript(js, null)
                                }
                            }
                        }
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
                        // Don't block main frame navigation, block 3rd party ads/trackers
                        if (request != null && !request.isForMainFrame && FilterMatcher.shouldBlock(url)) {
                            onBlockedRequest?.invoke()
                            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        currentTabId?.let { tabId ->
                            tabSessionManager.updateTabProgress(
                                tabId,
                                newProgress,
                                view?.canGoBack() ?: false,
                                view?.canGoForward() ?: false
                            )
                        }
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let { t ->
                            currentTabId?.let { tabId ->
                                val url = view?.url ?: ""
                                tabSessionManager.updateTabInfo(tabId, url, t)
                            }
                        }
                    }

                    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                        super.onReceivedIcon(view, icon)
                        icon?.let { bmp ->
                            scope.launch {
                                val color = AccentColorExtractor.extractAccentColor(bmp)
                                onAccentColorExtracted?.invoke(color)
                            }
                        }
                    }
                }
            }
            webView = wv
        }
        return webView!!
    }

    /**
     * নির্দিষ্ট ট্যাব আইডি এবং URL এর সাথে WebView বাইন্ড করে এবং লোড করে।
     */
    fun bindAndLoad(tabId: String, url: String) {
        val wv = getOrCreateWebView()
        if (currentTabId != tabId || wv.url != url) {
            currentTabId = tabId
            if (wv.url != url && url.isNotEmpty()) {
                wv.loadUrl(url)
            }
        }
    }

    fun loadUrl(url: String) {
        getOrCreateWebView().loadUrl(url)
    }

    fun goBack(): Boolean {
        val wv = webView ?: return false
        if (wv.canGoBack()) {
            wv.goBack()
            return true
        }
        return false
    }

    fun goForward(): Boolean {
        val wv = webView ?: return false
        if (wv.canGoForward()) {
            wv.goForward()
            return true
        }
        return false
    }

    fun reload() {
        webView?.reload()
    }

    fun stopLoading() {
        webView?.stopLoading()
    }

    /**
     * বর্তমান ওয়েব পেজকে অফলাইন পড়ার জন্য MHTML আর্কাইভ হিসেবে সেভ করে।
     */
    fun saveOfflineArchive(filePath: String, callback: (Boolean) -> Unit) {
        val wv = webView
        if (wv == null) {
            callback(false)
            return
        }
        try {
            wv.saveWebArchive(filePath, false) { savedPath ->
                callback(savedPath != null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            callback(false)
        }
    }

    /**
     * Fire Button বা সেশন ক্লিয়ারের জন্য সব কুকি, ক্যাশ এবং স্টোরেজ মুছে ফেলে।
     */
    fun clearAllData() {
        webView?.apply {
            clearCache(true)
            clearHistory()
            clearFormData()
        }
        CookieManager.getInstance().apply {
            removeAllCookies(null)
            flush()
        }
    }

    fun clearSessionData() {
        clearAllData()
    }

    fun destroy() {
        webView?.apply {
            stopLoading()
            clearHistory()
            loadUrl("about:blank")
            removeAllViews()
            destroy()
        }
        webView = null
        currentTabId = null
    }
}
