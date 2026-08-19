package com.com11h.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** Compatibility entry point. The standalone app keeps menu data locally. */
class MenuActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java).putExtra("screen", "menu"))
        finish()
    }
}
