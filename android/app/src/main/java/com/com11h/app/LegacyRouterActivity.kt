package com.com11h.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** Small navigation bridge while the native shell and existing business module coexist. */
class LegacyRouterActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen")
        when (screen) {
            "menu" -> startActivity(Intent(this, MenuActivity::class.java))
            "cart", "profile", "orders" -> startActivity(
                Intent(this, MainActivity::class.java).putExtra("screen", screen)
            )
            else -> startActivity(Intent(this, HomeActivity::class.java))
        }
        finish()
    }
}
