package com.com11h.app

import android.content.Context
import org.json.JSONObject

/** Lưu KCN khách đã chọn trên thiết bị. Không lưu dữ liệu nhạy cảm. */
object KcnStore {
    private const val PREFS = "com11h_local"
    private const val ID = "selected_kcn_id"
    private const val NAME = "selected_kcn_name"
    fun id(context: Context): Int = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(ID, 0)
    fun name(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(NAME, "") ?: ""
    fun save(context: Context, kcn: JSONObject) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(ID, kcn.optInt("id"))
            .putString(NAME, kcn.optString("name"))
            .apply()
    }
    fun clear(context: Context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(ID).remove(NAME).apply() }
}
