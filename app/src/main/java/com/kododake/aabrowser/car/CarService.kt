package com.kododake.aabrowser.car

import androidx.annotation.Keep
import com.google.android.apps.auto.sdk.CarActivity
import com.google.android.apps.auto.sdk.CarActivityService

/**
 * Entry point for the Android Auto **OEM projection** channel.
 *
 * This is the mechanism every working projected-WebView app (Screen2Auto,
 * WebViewAuto, slashmax/AABrowser, reactmap-android) uses to render an
 * arbitrary Activity surface on the head unit. Unlike the
 * `androidx.car.app.category.NAVIGATION` + `distractionOptimized` approach,
 * the projection channel is not subject to Android Auto's per-activity
 * distraction lockout, so the surface stays usable while the car is moving.
 *
 * Android Auto instantiates this service reflectively when it discovers the
 * `CATEGORY_PROJECTION` / `CATEGORY_PROJECTION_OEM` intent filter declared in
 * the manifest, so the class (and its no-arg constructor) must survive R8 —
 * hence [@Keep].
 */
@Keep
class CarService : CarActivityService() {
    override fun getCarActivity(): Class<out CarActivity> = CarBrowserActivity::class.java
}
