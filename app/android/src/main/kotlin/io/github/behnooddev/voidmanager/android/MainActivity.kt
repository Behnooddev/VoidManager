package io.github.behnooddev.voidmanager.android

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import io.github.behnooddev.voidmanager.shared.App
import io.github.behnooddev.voidmanager.shared.vault.VaultController

/** A FragmentActivity, because the biometric prompt is shown through a fragment. */
class MainActivity : FragmentActivity() {
    private val controller: VaultController
        get() = (application as VoidManagerApp).controller

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keeps vault contents out of screenshots, screen recordings and the recent apps overview.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        // The app is dark-first, so system bar icons are always drawn for a dark background.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { App(controller, deviceKeys = AndroidDeviceKeyProvider(this)) }
    }

    override fun onStart() {
        super.onStart()
        controller.onForegrounded()
    }

    override fun onStop() {
        // A rotation or another configuration change stops the activity briefly; that is not leaving the app.
        if (!isChangingConfigurations) controller.onBackgrounded()
        super.onStop()
    }
}
