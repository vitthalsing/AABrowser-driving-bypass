package com.kododake.aabrowser.car

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

/**
 * Acquires and holds media audio focus for the projected browser.
 *
 * On the projection surface, HTML5 `<video>` with sound pauses the instant it
 * starts — muted video plays fine because it never requests audio focus. That
 * points at focus: the WebView's own focus request isn't sustained, so
 * Chromium pauses. Proactively taking (and holding) `AUDIOFOCUS_GAIN` with a
 * listener that refuses to pause keeps sound — and therefore the video —
 * running.
 */
class AudioFocusHelper(context: Context) {

    private val audioManager =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Deliberately a no-op: we never pause playback on focus changes, so a
    // transient loss (e.g. a notification chime) does not stop the video.
    private val listener = AudioManager.OnAudioFocusChangeListener { }

    private val request =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            .setOnAudioFocusChangeListener(listener)
            .setWillPauseWhenDucked(false)
            .setAcceptsDelayedFocusGain(true)
            .build()

    fun acquire() {
        audioManager.requestAudioFocus(request)
    }

    fun release() {
        audioManager.abandonAudioFocusRequest(request)
    }
}
