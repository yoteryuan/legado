package io.legado.app.ui.book.read.ad

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import io.legado.app.utils.LogUtils

/**
 * 阅读页面广告管理器
 * 每20页显示一次谷歌广告
 */
class ReadAdManager private constructor() {

    companion object {
        const val AD_INTERVAL_PAGES = 20 // 每20页显示一次广告
        const val AD_DISPLAY_DURATION = 5000L // 广告显示5秒

        @Volatile
        private var instance: ReadAdManager? = null

        fun getInstance(): ReadAdManager {
            return instance ?: synchronized(this) {
                instance ?: ReadAdManager().also { instance = it }
            }
        }
    }

    private var adContainer: FrameLayout? = null
    private var webView: WebView? = null
    private var currentPageCount = 0
    private var isAdShowing = false
    private var onAdCloseListener: (() -> Unit)? = null

    /**
     * 初始化广告容器
     */
    fun initAdContainer(context: Context, container: FrameLayout) {
        adContainer = container
        setupWebView(context)
    }

    /**
     * 设置WebView
     */
    private fun setupWebView(context: Context) {
        webView = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                loadWithOverviewMode = true
                useWideViewPort = true
            }
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
        }
    }

    /**
     * 记录翻页，检查是否需要显示广告
     * @return true 表示显示了广告，false 表示没有显示
     */
    fun onPageTurn(): Boolean {
        if (isAdShowing) return false

        currentPageCount++
        LogUtils.d("ReadAdManager", "Page turned: $currentPageCount, interval: $AD_INTERVAL_PAGES")

        if (currentPageCount >= AD_INTERVAL_PAGES) {
            showAd()
            return true
        }
        return false
    }

    /**
     * 显示广告
     */
    private fun showAd() {
        if (isAdShowing || adContainer == null || webView == null) return

        isAdShowing = true
        currentPageCount = 0

        // 加载谷歌广告HTML
        val adHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { margin: 0; padding: 0; background: #f5f5f5; }
                    .ad-container { 
                        display: flex; 
                        justify-content: center; 
                        align-items: center; 
                        min-height: 100vh;
                        padding: 20px;
                    }
                    .close-btn {
                        position: fixed;
                        top: 10px;
                        right: 10px;
                        background: rgba(0,0,0,0.5);
                        color: white;
                        border: none;
                        padding: 8px 16px;
                        border-radius: 4px;
                        font-size: 14px;
                        cursor: pointer;
                        z-index: 9999;
                    }
                </style>
            </head>
            <body>
                <button class="close-btn" onclick="window.Android.closeAd()">关闭广告</button>
                <div class="ad-container">
                    <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-2786841664948982"
                         crossorigin="anonymous"></script>
                    <ins class="adsbygoogle"
                         style="display:block; text-align:center;"
                         data-ad-layout="in-article"
                         data-ad-format="fluid"
                         data-ad-client="ca-pub-2786841664948982"
                         data-ad-slot="6922581318"></ins>
                    <script>
                         (adsbygoogle = window.adsbygoogle || []).push({});
                    </script>
                </div>
            </body>
            </html>
        """.trimIndent()

        webView?.loadDataWithBaseURL("https://pagead2.googlesyndication.com", adHtml, "text/html", "UTF-8", null)

        // 添加WebView到容器
        adContainer?.removeAllViews()
        webView?.let { web ->
            adContainer?.addView(web, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
        }
        adContainer?.visibility = View.VISIBLE

        // 自动关闭广告（5秒后）
        adContainer?.postDelayed({
            closeAd()
        }, AD_DISPLAY_DURATION)

        LogUtils.d("ReadAdManager", "Ad shown")
    }

    /**
     * 关闭广告
     */
    fun closeAd() {
        if (!isAdShowing) return

        adContainer?.post {
            adContainer?.visibility = View.GONE
            adContainer?.removeAllViews()
            isAdShowing = false
            onAdCloseListener?.invoke()
            LogUtils.d("ReadAdManager", "Ad closed")
        }
    }

    /**
     * 设置广告关闭监听器
     */
    fun setOnAdCloseListener(listener: () -> Unit) {
        onAdCloseListener = listener
    }

    /**
     * 重置页数计数
     */
    fun resetPageCount() {
        currentPageCount = 0
    }

    /**
     * 获取当前页数
     */
    fun getCurrentPageCount(): Int = currentPageCount

    /**
     * 是否正在显示广告
     */
    fun isAdShowing(): Boolean = isAdShowing

    /**
     * 清理资源
     */
    fun destroy() {
        closeAd()
        webView?.destroy()
        webView = null
        adContainer = null
        instance = null
    }
}
