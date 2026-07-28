package com.kododake.aabrowser.car

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.webkit.WebView

/**
 * WebView tuned for the Android Auto projection surface.
 *
 * Chromium pauses HTML5 `<video>` the moment it believes its host window is no
 * longer visible. The projected surface (a virtual display) reports its window
 * as hidden as soon as playback starts, so video plays for a fraction of a
 * second and immediately pauses. Forcing the reported window visibility to
 * [View.VISIBLE] keeps the media pipeline running.
 *
 * This is the native half of the fix; the JavaScript Page Visibility spoof in
 * [CarBrowserActivity] handles sites (e.g. YouTube) that pause themselves via
 * `document.hidden` / `visibilitychange`.
 */
class CarWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : WebView(context, attrs, defStyleAttr) {

    override fun onWindowVisibilityChanged(visibility: Int) {
        // Never let the WebView see anything other than VISIBLE, so it doesn't
        // auto-pause media. GONE is still honoured so the view can be released.
        if (visibility != View.GONE) {
            super.onWindowVisibilityChanged(View.VISIBLE)
        } else {
            super.onWindowVisibilityChanged(visibility)
        }
    }
}
