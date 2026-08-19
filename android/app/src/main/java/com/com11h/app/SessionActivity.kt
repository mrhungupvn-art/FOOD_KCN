package com.com11h.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Activity nền tảng dùng chung cho TOÀN BỘ màn hình trong app.
 *
 * Yêu cầu nghiệp vụ: nếu khách đã đăng nhập mà quá 10 phút liên tục không có
 * thao tác nào trên app (không chạm màn hình, không mở lại app từ nền) thì
 * tự động đăng xuất, tương tự phiên đăng nhập trên web bị hết hạn.
 *
 * Cách hoạt động:
 *  - Mỗi lần khách chạm màn hình (bất kỳ đâu, kể cả trong WebView), Android
 *    gọi onUserInteraction() -> ta cập nhật mốc "lần hoạt động gần nhất".
 *  - Mỗi khi một Activity được đưa lên foreground (onResume) — kể cả khi mở
 *    app lại từ nền hoặc chuyển màn hình — ta so sánh với mốc đó. Nếu đã quá
 *    10 phút thì xoá token đăng nhập và đưa khách về Trang chủ.
 *  - NGOÀI RA: trong lúc app đang mở, ta còn chạy một vòng kiểm tra định kỳ
 *    (mỗi 15 giây) để phát hiện hết hạn NGAY CẢ KHI khách đứng yên mãi trên
 *    một màn hình duy nhất (không chuyển màn hình, không thoát app) — trường
 *    hợp này trước đây không được kiểm tra vì onResume() không được gọi lại
 *    khi Activity không hề bị pause.
 *
 * Toàn bộ Activity của app kế thừa lớp này thay vì Activity trực tiếp để
 * dùng chung một cơ chế duy nhất, không phải lặp lại logic ở từng màn hình.
 */
abstract class SessionActivity : Activity() {
    private lateinit var sessionAccount: AccountSync
    private val sessionHandler = Handler(Looper.getMainLooper())
    private var sessionWatchdog: Runnable? = null

    companion object {
        /** Tần suất kiểm tra hết hạn phiên trong lúc app đang mở (foreground). */
        private const val WATCHDOG_INTERVAL_MS = 15_000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionAccount = AccountSync(this)
    }

    override fun onResume() {
        super.onResume()
        if (checkSessionExpiry()) return
        sessionAccount.touch()
        startSessionWatchdog()
    }

    override fun onPause() {
        stopSessionWatchdog()
        super.onPause()
    }

    /**
     * Kiểm tra hết hạn phiên. Trả về true nếu đã xử lý đăng xuất (activity
     * hiện tại sắp bị finish() để chuyển về Trang chủ), false nếu phiên vẫn
     * còn hợp lệ (hoặc chưa đăng nhập).
     */
    private fun checkSessionExpiry(): Boolean {
        if (!(sessionAccount.isLoggedIn() && sessionAccount.isSessionExpired())) return false
        sessionAccount.logout()
        Toast.makeText(
            this,
            "Phiên đăng nhập đã hết hạn do không hoạt động quá 10 phút. Vui lòng đăng nhập lại.",
            Toast.LENGTH_LONG
        ).show()
        if (this !is HomeActivity) {
            startActivity(
                Intent(this, HomeActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
        } else {
            // Đang đứng ngay trên Trang chủ khi phiên hết hạn (ví dụ do watchdog
            // phát hiện giữa lúc đứng yên) -> không cần chuyển màn hình, chỉ cần
            // báo cho màn hình tự cập nhật lại giao diện (ẩn icon đã đăng nhập...).
            onSessionExpired()
        }
        return true
    }

    /**
     * Được gọi khi phiên vừa bị đăng xuất tự động NGAY TRÊN màn hình hiện tại
     * (không phải do chuyển màn hình). Activity con có thể override để tự làm
     * mới giao diện (vd: ẩn dấu chấm xanh ở icon Tài khoản). Mặc định không
     * làm gì vì hầu hết trường hợp đã được xử lý bằng cách quay về Trang chủ.
     */
    protected open fun onSessionExpired() {}

    /** Bắt đầu vòng lặp kiểm tra định kỳ trong lúc Activity đang ở foreground. */
    private fun startSessionWatchdog() {
        stopSessionWatchdog()
        val runnable = object : Runnable {
            override fun run() {
                if (isFinishing || isDestroyed) return
                if (!checkSessionExpiry()) {
                    sessionHandler.postDelayed(this, WATCHDOG_INTERVAL_MS)
                }
            }
        }
        sessionWatchdog = runnable
        sessionHandler.postDelayed(runnable, WATCHDOG_INTERVAL_MS)
    }

    private fun stopSessionWatchdog() {
        sessionWatchdog?.let { sessionHandler.removeCallbacks(it) }
        sessionWatchdog = null
    }

    /** Gọi mỗi khi khách chạm/bấm bất kỳ đâu trong Activity đang mở (kể cả WebView). */
    override fun onUserInteraction() {
        super.onUserInteraction()
        sessionAccount.touch()
    }
}
