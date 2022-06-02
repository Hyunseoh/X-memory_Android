package com.example.x_memory

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.x_memory.databinding.ActivityAuthBinding
import kotlinx.android.synthetic.main.activity_auth.*
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import kotlin.math.log

private lateinit var binding: ActivityAuthBinding

class AuthActivity : AppCompatActivity() {
    var login:Login? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val client = OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(CookieManager())) //쿠키매니저 연결
            .build()

        var retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl("http://xmemory.thdus.net")
//            .baseUrl("http://121.172.128.251:8000")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        var loginService: LoginService = retrofit.create(LoginService::class.java)


        binding.signInButton.setOnClickListener{
            var text1 = login_id.text.toString()
            var text2 = login_paw.text.toString()
//            val token = "Token " + SharedPreferences.prefs.getString("token", "")
            loginService.requestLogin(text1,text2).enqueue(object: Callback<Login> {
                override fun onFailure(call: Call<Login>, t: Throwable) {

                    var dialog = AlertDialog.Builder(this@AuthActivity)
                    dialog.setTitle("에러")
                    dialog.setMessage("호출실패했습니다.")
                    dialog.show()
                }

                override fun onResponse(call: Call<Login>, response: Response<Login>) {
                    login = response.body()
                    Log.d("LOGIN","msg : "+login?.msg)
                    Log.d("LOGIN","code : "+login?.code)
                    // 코드 200번이 들어왔을 때 로그인 성공
                    if( login?.code == "200") {
                        //id, token 저장
                        SharedPreferences.prefs.setString("id", text1)
                        login?.token?.let { it1 -> SharedPreferences.prefs.setString("token", it1) }

                        Toast.makeText(applicationContext, "환영합니다!", Toast.LENGTH_SHORT).show()
                        val i = Intent(this@AuthActivity, MainActivity::class.java)
                        startActivity(i)
                        finish()
                    }
                    else {
                        Toast.makeText(applicationContext, "잘못된 정보입니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }
}

//class AuthActivity : AppCompatActivity() {
//    var login:Login? = null
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivityAuthBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        val client = OkHttpClient.Builder()
//            .cookieJar(JavaNetCookieJar(CookieManager())) //쿠키매니저 연결
//            .build()
//
//        var retrofit = Retrofit.Builder()
//            .client(client)
//            .baseUrl("http://172.30.1.49:8000")
////            .baseUrl("http://xmemory.thdus.net")
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//        var loginService: LoginService = retrofit.create(LoginService::class.java)
//
//
//        binding.signInButton.setOnClickListener{
//            var text1 = login_id.text.toString()
//            var text2 = login_paw.text.toString()
//
//            loginService.requestLogin(text1,text2).enqueue(object: Callback<Login> {
//                override fun onFailure(call: Call<Login>, t: Throwable) {
//
//                    var dialog = AlertDialog.Builder(this@AuthActivity)
//                    dialog.setTitle("에러")
//                    dialog.setMessage("호출실패했습니다.")
//                    dialog.show()
//                }
//
//                override fun onResponse(call: Call<Login>, response: Response<Login>) {
//                    login = response.body()
//                    Log.d("LOGIN","msg : "+login?.msg)
//                    Log.d("LOGIN","code : "+login?.code)
//                    if( login?.code == "0000") {
//                        Toast.makeText(applicationContext, "환영합니다!", Toast.LENGTH_SHORT).show()
//                        val i = Intent(this@AuthActivity, MainActivity::class.java)
//                        startActivity(i)
//                        finish()
//                    }
//                    else {
//                        Toast.makeText(applicationContext, "잘못된 정보입니다.", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            })
//        }
//    }
//}
