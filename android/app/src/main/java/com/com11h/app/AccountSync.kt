package com.com11h.app

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * The ONLY remote integration used by the standalone COM11H app.
 * Menu, cart and app orders stay local. Customer account/profile uses the
 * existing COM11H account API and preserves the legacy token storage key
 * used by the previous app, so an existing app session can be reused.
 */
class AccountSync(context: Context) {
    companion object {
        private const val BASE_URL = "https://com11h.com/api/index.php"
        private const val PREFS = "com11h_secure"
        private const val TOKEN = "token"
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun token(): String? = prefs.getString(TOKEN, null)
    fun isLoggedIn(): Boolean = !token().isNullOrBlank()
    fun saveToken(value: String) = prefs.edit().putString(TOKEN, value).apply()
    fun logout() = prefs.edit().remove(TOKEN).apply()

    fun request(action: String, method: String = "GET", body: String? = null): JSONObject {
        val url = URL("$BASE_URL?action=${URLEncoder.encode(action, "UTF-8")}")
        val c = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12000
            readTimeout = 15000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            token()?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
        return try {
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            val json = try { JSONObject(text) } catch (_: Exception) {
                JSONObject().put("ok", false).put("message", "Máy chủ trả về dữ liệu không hợp lệ (HTTP $code).")
            }
            if (!json.has("http_code")) json.put("http_code", code)
            json
        } finally { c.disconnect() }
    }
}
