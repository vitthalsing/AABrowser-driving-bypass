package com.kododake.aabrowser.car

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.PowerManager

/**
 * Keeps media playing on the projected surface, mirroring exactly what the
 * shipping `MainActivity` does for the phone browser:
 *
 *  1. Audio focus: request `AUDIOFOCUS_GAIN` and **re-request on any loss**, so
 *     the OS treats the app as an active media session and never leaves it
 *     without focus. This is the piece that was missing — unmuted video paused
 *     because nothing held focus once the WebView's own request dropped.
 *  2. Wake lock: a partial wake lock so JS timers keep firing and the CPU stays
 *     awake during playback.
 *
 * These run alongside the JavaScript pause-guard / silent-oscillator injected
 * by the shared `configureWebView`; the phone browser uses the native and JS
 * layers together, so they do not conflict.
 */
class CarMediaKeepAlive(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null

    fun acquire() {
        acquireAudioFocus()
        acquireWakeLock()
    }

    fun release() {
        releaseAudioFocus()
        releaseWakeLock()
    }

    private fun acquireAudioFocus() {
        val am = audioManager ?: return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener { focusChange ->
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                    focusChange == AudioManager.AUDIOFOCUS_LOSS
                ) {
                    // Re-request focus after a loss so playback is never left
                    // without an owning media session.
                    acquireAudioFocus()
                }
            }
            .build()
        audioFocusRequest = request
        am.requestAudioFocus(request)
    }

    private fun releaseAudioFocus() {
        val am = audioManager ?: return
        audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AABrowser:CarVideoPlayback"
        ).also { it.acquire(4 * 60 * 60 * 1000L /* 4 hours */) }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }
}
