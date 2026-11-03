/*
 * Zalith Launcher Lite - Game surface registry
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.movtery.zalithlauncher.recorder

import android.view.View
import java.lang.ref.WeakReference

/**
 * Holds a weak reference to the actual Minecraft render view so recording captures
 * the game surface rather than a separate virtual display of the Android screen.
 */
object GameSurfaceRegistry {
    @Volatile private var viewRef: WeakReference<View>? = null

    fun register(view: View) {
        viewRef = WeakReference(view)
    }

    fun unregister() {
        viewRef?.clear()
        viewRef = null
    }

    fun getView(): View? = viewRef?.get()
}
