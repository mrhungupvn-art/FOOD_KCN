package com.com11h.app

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*

/**
 * Live commerce surface. The app intentionally reuses the production COM11H
 * menu/cart/order pages so web-admin changes are immediately reflected in Android.
 */
class MenuActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var progressBar: ProgressBar
    private var firstLoad = true

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookieManager.getInstance().setAcceptCookie(true)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(247, 249, 247)) }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            setBackgroundColor(Color.WHITE)
            elevation = dp(4).toFloat()
        }
        toolbar.addView(TextView(this).apply {
            text = "‹"; textSize = 34f; gravity = Gravity.CENTER; setTextColor(Color.rgb(22,128,60));
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(44), dp(48)))
        toolbar.addView(TextView(this).apply {
            text = "🍚  THỰC ĐƠN COM11H"; textSize = 17f; setTextColor(Color.rgb(22,128,60)); setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        toolbar.addView(TextView(this).apply {
            text = "↻"; textSize = 28f; gravity = Gravity.CENTER; setTextColor(Color.rgb(22,128,60));
            setOnClickListener { web.reload() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        root.addView(toolbar)

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { visibility = View.GONE; max = 100 }
        root.addView(progressBar, LinearLayout.LayoutParams(-1, dp(3)))

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val host = request.url.host ?: return false
                    return if (host.equals("com11h.com", true) || host.endsWith(".com11h.com", true)) false else {
                        Toast.makeText(this@MenuActivity, "Liên kết ngoài đã được chặn trong app.", Toast.LENGTH_SHORT).show(); true
                    }
                }
                override fun onPageFinished(view: WebView, url: String) {
                    if (firstLoad) { firstLoad = false }
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                    if (request.isForMainFrame) Toast.makeText(this@MenuActivity, "Không tải được COM11H. Kiểm tra kết nối mạng rồi bấm ↻.", Toast.LENGTH_LONG).show()
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    progressBar.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
                    progressBar.progress = newProgress
                }
            }
        }
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        web.loadUrl("https://com11h.com/menu.php")
    }

    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::web.isInitialized) { web.stopLoading(); web.destroy() }
        super.onDestroy()
    }
}
