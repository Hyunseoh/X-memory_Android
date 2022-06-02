package com.example.x_memory

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import com.example.x_memory.databinding.ActivityProfileBinding
import kotlinx.android.synthetic.main.activity_profile.*
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.internal.notifyAll
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager


class ProfileActivity : AppCompatActivity() {
    
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 저장된 id,token 불러오기
        val userID = SharedPreferences.prefs.getString("id", "")
        val token = "Token " + SharedPreferences.prefs.getString("token", "")
        binding.accountName.text = userID
        val tag = ""

        // retrofit
        val client = OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(CookieManager())) //쿠키매니저 연결
            .build()

        var retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl("http://xmemory.thdus.net")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        var profileService: ProfileService = retrofit.create(ProfileService::class.java)

        profileService.requestProfile(token, tag).enqueue(object: Callback<Profile> {
            override fun onFailure(call: Call<Profile>, t: Throwable) {

                var dialog = AlertDialog.Builder(this@ProfileActivity)
                dialog.setTitle("에러")
                dialog.setMessage("호출실패했습니다.")
                dialog.show()
            }

            override fun onResponse(call: Call<Profile>, response: Response<Profile>) {
                var profile = response.body()
                if( profile?.code == "200") {
                    binding.accountCount.text = profile?.count.toString()
//                    val i = Intent(this@ProfileActivity, ProfileActivity::class.java)
//                    startActivity(i)
//                    finish()
                }
                else {
                    Toast.makeText(applicationContext, "저장 실패", Toast.LENGTH_SHORT).show()
                }
            }
        })


        // 웹뷰 추가
        binding.accountName.text = userID
        var myWebView: WebView = findViewById(R.id.webview)

        myWebView.settings.javaScriptEnabled
        myWebView.settings.domStorageEnabled
        myWebView.settings.builtInZoomControls
        myWebView.settings.displayZoomControls

        val extraHeaders: MutableMap<String, String> = mutableMapOf()
        extraHeaders.put("Authorization", token)

        myWebView?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                url: String?
            ): Boolean {
                if (url != null) {
                    view!!.loadUrl(url, extraHeaders)
                }
                return false
            }
        }
        myWebView.loadUrl("http://xmemory.thdus.net/app_index/", extraHeaders)
        myWebView.reload()

        binding.btnLogout.setOnClickListener {
            logout_function()
        }
    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_tab, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item?.itemId) {
            R.id.signOut_button -> {
                logout_function()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun logout_function(){
        val i = Intent(this@ProfileActivity, AuthActivity::class.java)
        startActivity(i)
        finish()
//        AWSMobileClient.getInstance().initialize(
//            applicationContext,
//            object : Callback<UserStateDetails?> {
//                override fun onResult(userStateDetails: UserStateDetails?) {
//                    // 로그아웃 후 로그인 창으로 이동
//                    AWSMobileClient.getInstance().signOut()
//                    val i = Intent(this@ProfileActivity, AuthActivity::class.java)
//                    startActivity(i)
//                    finish()
//                }
//
//                override fun onError(e: Exception) {}
//            })
    }
}