package com.kododake.aabrowser.car

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.annotation.Keep
import com.google.android.apps.auto.sdk.CarActivity
import com.kododake.aabrowser.R
import com.kododake.aabrowser.data.BrowserPreferences

/**
 * The projected browser surface shown on the head unit via the OEM projection
 * channel (see [CarService]).
 *
 * MVP scope: a single full-screen [WebView] — just enough to verify the app
 * opens and stays usable while driving (i.e. is not covered by Android Auto's
 * grey distraction scrim). Tabs, the URL bar, settings and the rest of the
 * phone UI live in `MainActivity` and are not ported here yet.
 *
 * Notes on the SDK:
 *  - [CarActivity] is NOT an `android.content.Context`; obtain a real context
 *    from the inflated [WebView] via [WebView.getContext].
 *  - There is no `onDestroy` hook in the SDK surface, so the media-playback
 *    foreground service is bound to the [onStart]/[onStop] pair.
 *  - The class is instantiated reflectively by the SDK, so keep it for R8.
 */
@Keep
class CarBrowserActivity : CarActivity() {

    private lateinit var carRoot: FrameLayout
    private lateinit var webView: WebView

    // Fullscreen (<video>) support
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Avoid the SDK tearing down/recreating the surface on config changes,
        // which would interrupt playback.
        setIgnoreConfigChanges(0xFFFF)
        setContentView(R.layout.activity_car_browser)

        carRoot = findViewById(R.id.car_root) as FrameLayout
        webView = findViewById(R.id.car_webview) as WebView

        configureWebView(webView)

        // Hide the projected chrome so the browser fills the head-unit screen.
        runCatching {
            carUiController.menuController.hideMenuButton()
            carUiController.statusBarController.hideTitle()
        }

        val appContext = webView.context.applicationContext
        val startUrl = BrowserPreferences.resolveInitialUrl(appContext)
        webView.loadUrl(startUrl)
    }

    @Suppress("SetJavaScriptEnabled")
    private fun configureWebView(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            // Allow autoplay so video keeps rolling without a tap.
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                url?.let { BrowserPreferences.persistUrl(view.context.applicationContext, it) }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) {
                    callback.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback
                webView.visibility = View.GONE
                carRoot.addView(
                    view,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }

            override fun onHideCustomView() {
                val view = customView ?: return
                carRoot.removeView(view)
                customView = null
                webView.visibility = View.VISIBLE
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ForegroundService.start(webView.context.applicationContext)
    }

    override fun onStop() {
        // Deliberately do NOT call webView.onPause(): pausing here would stop
        // media playback, which defeats the purpose of the projection surface.
        ForegroundService.stop(webView.context.applicationContext)
        super.onStop()
    }

    override fun onBackPressed() {
        when {
            customView != null -> webView.webChromeClient?.onHideCustomView()
            webView.canGoBack() -> webView.goBack()
            else -> super.onBackPressed()
        }
    }
}
