package com.foodkcn.app

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.HandlerThread
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Lấy toạ độ GPS gần nhất đã biết của thiết bị, dùng để app tự gửi kèm
 * lat/lng khi gọi API 'menu' (xem MainActivity.showMenu) — nhờ đó server
 * tính được km và phí ship cho TỪNG món ngay trên màn Thực đơn, giống hệt
 * cách trang web (WebActivity/menu.php) đã làm qua navigator.geolocation.
 *
 * Cố tình dùng LocationManager thuần (không phụ thuộc Google Play Services)
 * vì app không khai báo play-services-location — chỉ cần toạ độ "gần đúng
 * gần nhất" (last known), không cần theo dõi vị trí liên tục.
 */
object LocationHelper {
    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Trả về toạ độ gần nhất đã biết (từ GPS hoặc mạng), hoặc null nếu chưa có/không có quyền. */
    fun lastKnownLocation(context: Context): Location? {
        if (!hasPermission(context)) return null
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val providers = lm.getProviders(true)
            var best: Location? = null
            for (provider in providers) {
                val loc = try { lm.getLastKnownLocation(provider) } catch (_: SecurityException) { null } ?: continue
                if (best == null || loc.time > (best?.time ?: 0L)) best = loc
            }
            best
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Ưu tiên toạ độ đã biết (nhanh, tức thì). Nếu máy CHƯA từng có toạ độ nào
     * cả (ví dụ vừa cấp quyền định vị lần đầu, hoặc mới bật GPS lần đầu) thì
     * xin một lần đo GPS/mạng "tươi", chờ tối đa [timeoutMs] rồi thôi — không
     * để khách phải chờ vô hạn nếu không bắt được sóng định vị.
     *
     * PHẢI gọi trên luồng nền (không gọi trên main thread) vì có chờ (block).
     */
    fun currentLocationBlocking(context: Context, timeoutMs: Long = 6000L): Location? {
        lastKnownLocation(context)?.let { return it }
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = try { lm.getProviders(true) } catch (_: Exception) { emptyList<String>() }
        if (providers.isEmpty()) return null

        var result: Location? = null
        val latch = CountDownLatch(1)
        val thread = HandlerThread("food-kcn-location-fix").apply { start() }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) { result = location; latch.countDown() }
            @Deprecated("deprecated in API") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        try {
            providers.forEach { provider ->
                try { lm.requestLocationUpdates(provider, 0L, 0f, listener, thread.looper) } catch (_: Exception) {}
            }
            latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
            // bỏ qua — trả về null bên dưới nếu không kịp có toạ độ
        } finally {
            try { lm.removeUpdates(listener) } catch (_: Exception) {}
            thread.quitSafely()
        }
        return result
    }
}
