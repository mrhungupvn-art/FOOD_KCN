package com.com11h.app.shoplive

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import com.com11h.app.AccountActivity
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executors

/** COM11H LIVE shopping shell. Customer + existing Partner/Seller flows. */
class ShopLiveActivity : Activity() {
    private lateinit var content: LinearLayout
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var api: ShopLiveApi
    private val cart = linkedMapOf<Long, CartItem>()
    private val vnd = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    private var seller: SellerSync.SellerContext? = null

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); api = ShopLiveApi(this); buildShell(); showHome() }
    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }

    private fun buildShell() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        root.addView(TextView(this).apply { text = "COM11H LIVE"; textSize = 21f; setTextColor(Color.WHITE); gravity = Gravity.CENTER_VERTICAL; setPadding(dp(22),0,dp(22),0); setBackgroundColor(Color.rgb(235,78,35)) }, LinearLayout.LayoutParams(-1, dp(58)))
        val scroll = ScrollView(this); content = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(14),dp(12),dp(14),dp(90)) }; scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
        val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setBackgroundColor(Color.WHITE);elevation=12f}
        listOf("🏠\nHome","🛍️\nShop","🔴\nLIVE","🛒\nGiỏ","👤\nTôi").forEachIndexed{i,label->nav.addView(Button(this).apply{text=label;textSize=11f;setAllCaps(false);setPadding(0,4,0,4);setOnClickListener{when(i){0->showHome();1->showShop();2->showLive();3->showCart();else->showAccount()}}},LinearLayout.LayoutParams(0,dp(62),1f))}
        root.addView(nav); setContentView(root)
    }
    private fun showHome(){clear("Xin chào 👋");addHero("COM11H LIVE","Xem LIVE • Ghim món • Mua ngay");section("🔴 LIVE ĐANG DIỄN RA");addButton("Xem tất cả LIVE",235){showLive()}}
    private fun showShop(){clear("🛍️ Cửa hàng");val search=EditText(this).apply{hint="Tìm món ăn..."};content.addView(search,lp());addButton("🔎 Tìm kiếm",235){loadProducts(search.text.toString())};section("SẢN PHẨM");loadProducts()}
    private fun showLive(){clear("🔴 LIVE");addHero("LIVE SHOPPING","Xem người bán và đặt món ngay trong LIVE");addButton("🔴 Seller: Bắt đầu LIVE",235){startSellerLive()};section("LIVE ĐANG DIỄN RA");loadLiveRooms()}
    private fun showCart(){clear("🛒 Giỏ hàng");if(cart.isEmpty()){addText("Giỏ hàng đang trống.",17f);return};cart.values.forEach{item->addCard("${item.product.name}\n${money(item.product.priceVnd)} × ${item.quantity}"){cart.remove(item.product.id);showCart()}};addText("Tổng: ${money(cart.values.sumOf{it.subtotalVnd})}",19f);addButton("Đặt hàng",235){placeOrder()}}
    private fun showAccount(){clear("👤 Tài khoản");if(api.isLoggedIn()){addButton("Đăng xuất",45){api.logout();showAccount()};section("SELLER");addButton("Kiểm tra quyền Seller",235){loadSellerContext()}}else{addText("Đăng nhập bằng tài khoản COM11H hiện tại.",17f);addButton("Mở đăng nhập COM11H",235){startActivity(android.content.Intent(this,AccountActivity::class.java))}}}

    private fun loadProducts(q:String?=null){addText("Đang tải...",14f);executor.execute{val r=runCatching{api.products(q=q)}.getOrElse{JSONObject().put("ok",false)};val a=ShopLiveApi.jsonArray(r,"products");runOnUiThread{if(content.childCount>0)content.removeViewAt(content.childCount-1);if(a.length()==0)addText("Chưa có sản phẩm.",15f) else (0 until a.length()).forEach{i->a.optJSONObject(i)?.let{o->val p=Product(o.optLong("id"),o.optLong("shop_id"),o.optString("name"),o.optLong("price_vnd",o.optLong("price")),o.optString("image_url").ifBlank{null},o.optInt("stock"));addProductCard(p)}}}}}
    private fun loadLiveRooms(){addText("Đang tải LIVE...",14f);executor.execute{val r=runCatching{api.liveRooms()}.getOrElse{JSONObject()};val a=ShopLiveApi.jsonArray(r,"live_rooms");runOnUiThread{if(content.childCount>0)content.removeViewAt(content.childCount-1);if(a.length()==0)addText("Chưa có LIVE đang diễn ra.",15f) else (0 until a.length()).forEach{i->a.optJSONObject(i)?.let{o->openRoomCard(LiveRoom(o.optLong("id"),o.optLong("shop_id"),o.optString("shop_name"),o.optString("title"),o.optString("cover_url").ifBlank{null},o.optString("stream_url").ifBlank{null},o.optString("status","live"),o.optInt("viewer_count"),o.optLong("pinned_product_id").takeIf{it>0}))}}}}}
    private fun openRoomCard(r:LiveRoom){addCard("🔴 ${r.shopName}\n${r.title}\n👁 ${r.viewerCount} đang xem"){openLive(r)}}
    private fun addProductCard(p:Product)=addCard("🛍️ ${p.name}\n${money(p.priceVnd)} • Kho ${p.stock}"){val n=cart[p.id]?.quantity?:0;cart[p.id]=CartItem(p,n+1);Toast.makeText(this,"Đã thêm vào giỏ",Toast.LENGTH_SHORT).show()}
    private fun openLive(room:LiveRoom){clear("🔴 ${room.shopName}");addText(room.title,21f);addText("👁 ${room.viewerCount} người đang xem",13f);if(!room.streamUrl.isNullOrBlank()){val video=VideoView(this).apply{setBackgroundColor(Color.BLACK);setVideoPath(room.streamUrl);setOnPreparedListener{it.start()}};content.addView(video,LinearLayout.LayoutParams(-1,dp(220)))}else addText("Đang chờ Live Server cấp stream URL.",15f);section("📌 SẢN PHẨM ĐANG GHIM");addButton("🛒 Mua sản phẩm LIVE",235){showShop()};section("💬 CHAT LIVE");val chat=EditText(this).apply{hint="Viết bình luận..."};content.addView(chat,lp());addButton("Gửi",235){if(chat.text.isNotBlank()){val msg=chat.text.toString();chat.text.clear();executor.execute{runCatching{api.sendLiveMessage(room.id,msg)}}}}}

    private fun loadSellerContext(){executor.execute{val r=runCatching{api.sellerContext()}.getOrElse{JSONObject()};val d=r.optJSONObject("data")?:r;runOnUiThread{if(!r.optBoolean("ok",false)||!d.optBoolean("is_seller",false)){addText("Tài khoản này chưa được cấp quyền Seller/Đối tác.",16f);return@runOnUiThread};seller=SellerSync(this).context();clear("🏪 SELLER CENTER");addText("Xin chào ${seller?.displayName?:"Seller"}",21f);seller?.shops?.forEach{shop->addCard("🏪 ${shop.name}\nTrạng thái: ${shop.status}"){sellerShopMenu(shop)}}}}}
    private fun sellerShopMenu(shop:SellerSync.SellerShop){clear("🏪 ${shop.name}");addText("Seller Center",21f);addButton("🔴 Tạo LIVE",235){sellerCreateLive(shop)};addButton("🍱 Chọn sản phẩm LIVE",45){showShop()};addText("Các chức năng Voucher, Flash Sale, ghim sản phẩm và thống kê sẽ được mở theo API production.",14f)}
    private fun sellerCreateLive(shop:SellerSync.SellerShop){clear("🔴 Tạo LIVE");val title=EditText(this).apply{hint="Tiêu đề LIVE"};content.addView(title,lp());addButton("BẮT ĐẦU LIVE",235){val t=title.text.toString().trim().ifBlank{"LIVE tại ${shop.name}"};executor.execute{val r=runCatching{api.startLive(shop.id,t)}.getOrElse{JSONObject().put("ok",false).put("message",it.message?:"Lỗi")};runOnUiThread{Toast.makeText(this,r.optString("message","Đã gửi yêu cầu"),Toast.LENGTH_LONG).show();showLive()}}}}
    private fun startSellerLive(){if(!api.isLoggedIn()){showAccount();return};loadSellerContext()}
    private fun placeOrder(){if(!api.isLoggedIn())showAccount()else Toast.makeText(this,"Đơn hàng sẽ đi qua Order API COM11H hiện tại.",Toast.LENGTH_LONG).show()}
    private fun clear(t:String){content.removeAllViews();addText(t,24f)}
    private fun section(t:String){addText(t,18f);addSpace(5)}
    private fun addHero(t:String,s:String){content.addView(TextView(this).apply{text="$t\n$s";textSize=20f;setTextColor(Color.WHITE);setPadding(dp(18),dp(18),dp(18),dp(18));setBackgroundColor(Color.rgb(235,78,35))},lp())}
    private fun addText(t:String,size:Float){content.addView(TextView(this).apply{text=t;textSize=size;setTextColor(Color.rgb(35,35,35));setPadding(4,9,4,9)},lp())}
    private fun addCard(t:String,click:()->Unit){content.addView(Button(this).apply{text=t;textSize=15f;setAllCaps(false);gravity=Gravity.START or Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(10),dp(14),dp(10));setOnClickListener{click()}},lp())}
    private fun addButton(t:String,colorSeed:Int,click:()->Unit){content.addView(Button(this).apply{text=t;setTextColor(Color.WHITE);setBackgroundColor(if(colorSeed==235)Color.rgb(235,78,35)else Color.DKGRAY);setAllCaps(false);setOnClickListener{click()}},LinearLayout.LayoutParams(-1,dp(50)).apply{setMargins(0,6,0,6)})}
    private fun addSpace(h:Int){content.addView(Space(this),LinearLayout.LayoutParams(1,dp(h)))}
    private fun lp()=LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT).apply{setMargins(0,5,0,5)}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun money(v:Long)="${vnd.format(v)} đ"
}
