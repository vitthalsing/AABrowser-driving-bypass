package com.kododake.aabrowser.car

import android.content.Context
import android.os.PowerManager

/**
 * Keeps the CPU awake on the projected surface so JS timers keep firing and
 * playback isn't throttled, mirroring the wake lock the phone browser
 * (`MainActivity`) holds.
 *
 * Deliberately does NOT grab app-level audio focus. The WebView's own Chromium
 * engine owns media audio focus; an app-level `AUDIOFOCUS_GAIN` (re-requested
 * on loss) fights it and pauses unmuted video the instant it starts. The JS
 * silent-oscillator injected by the shared `configureWebView` is what holds
 * audio focus across driving-state transitions.
 */
class CarMediaKeepAlive(context: Context) {

    private val appContext = context.applicationContext
    private var wakeLock: PowerManager.WakeLock? = null

    fun acquire() {
        if (wakeLock?.isHeld == true) return
        val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AABrowser:CarVideoPlayback"
        ).also { it.acquire(4 * 60 * 60 * 1000L /* 4 hours */) }
    }

    fun release() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }
}
