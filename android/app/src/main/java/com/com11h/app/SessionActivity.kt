package com.com11h.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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
 *
 * Toàn bộ Activity của app kế thừa lớp này thay vì Activity trực tiếp để
 * dùng chung một cơ chế duy nhất, không phải lặp lại logic ở từng màn hình.
 */
abstract class SessionActivity : Activity() {
    private lateinit var sessionAccount: AccountSync

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionAccount = AccountSync(this)
    }

    override fun onResume() {
        super.onResume()
        if (sessionAccount.isLoggedIn() && sessionAccount.isSessionExpired()) {
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
                return
            }
        }
        sessionAccount.touch()
    }

    /** Gọi mỗi khi khách chạm/bấm bất kỳ đâu trong Activity đang mở (kể cả WebView). */
    override fun onUserInteraction() {
        super.onUserInteraction()
        sessionAccount.touch()
    }
}
