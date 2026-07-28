package com.kododake.aabrowser.car

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.annotation.Keep
import com.google.android.apps.auto.sdk.CarActivity
import com.kododake.aabrowser.R
import com.kododake.aabrowser.data.BrowserPreferences
import com.kododake.aabrowser.web.BrowserCallbacks
import com.kododake.aabrowser.web.configureWebView

/**
 * The projected browser surface shown on the head unit via the OEM projection
 * channel (see [CarService]).
 *
 * MVP scope: a single full-screen [WebView] — just enough to verify the app
 * opens and stays usable while driving (i.e. is not covered by Android Auto's
 * grey distraction scrim). Tabs, the URL bar, settings and the rest of the
 * phone UI live in `MainActivity` and are not ported here yet.
 *
 * Crucially, the WebView is configured through the shared
 * [com.kododake.aabrowser.web.configureWebView], so it inherits the app's
 * proven media handling — including the VIDEO_PAUSE_GUARD_JS that suppresses
 * system-driven pauses and holds audio focus via a silent Web Audio
 * oscillator. Rolling a bespoke WebView here is what previously let video pause
 * as soon as sound started.
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
    private var keepAlive: CarMediaKeepAlive? = null

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

        keepAlive = CarMediaKeepAlive(webView.context)

        configureWebView(
            webView = webView,
            callbacks = BrowserCallbacks(
                onUrlChange = { url ->
                    BrowserPreferences.persistUrl(webView.context.applicationContext, url)
                },
                onEnterFullscreen = { view, callback -> enterFullscreen(view, callback) },
                onExitFullscreen = { exitFullscreen() }
            )
        )

        // Hide the projected chrome so the browser fills the head-unit screen.
        runCatching {
            carUiController.menuController.hideMenuButton()
            carUiController.statusBarController.hideTitle()
        }

        val appContext = webView.context.applicationContext
        val startUrl = BrowserPreferences.resolveInitialUrl(appContext)
        webView.loadUrl(startUrl)
    }

    private fun enterFullscreen(view: View, callback: WebChromeClient.CustomViewCallback) {
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

    private fun exitFullscreen() {
        val view = customView ?: return
        carRoot.removeView(view)
        customView = null
        webView.visibility = View.VISIBLE
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
    }

    override fun onStart() {
        super.onStart()
        keepAlive?.acquire()
        webView.onResume()
        webView.resumeTimers()
        ForegroundService.start(webView.context.applicationContext)
    }

    override fun onStop() {
        // Deliberately do NOT call webView.onPause(): pausing here would stop
        // media playback, which defeats the purpose of the projection surface.
        keepAlive?.release()
        ForegroundService.stop(webView.context.applicationContext)
        super.onStop()
    }

    override fun onBackPressed() {
        when {
            customView != null -> exitFullscreen()
            webView.canGoBack() -> webView.goBack()
            else -> super.onBackPressed()
        }
    }
}
