package com.example.x_memory

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
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
import java.io.File
import java.net.CookieManager
import java.text.SimpleDateFormat


class ProfileActivity : AppCompatActivity() {

    private val CAMERA = 100
    private val GALLERY = 200


    private var photoUri: Uri? = null

    // Permisisons
    val PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    
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
                dialog.setMessage("호출에 실패했습니다")
                dialog.show()
            }

            override fun onResponse(call: Call<Profile>, response: Response<Profile>) {
                var profile = response.body()
                if( profile?.code == "200") {
                    binding.accountCount.text = profile?.count.toString()
                }
                else {
                    Toast.makeText(applicationContext, "저장 실패", Toast.LENGTH_SHORT).show()
                }
            }
        })


        // 웹뷰 추가
        binding.accountName.text = userID
        var myWebView: WebView = findViewById(R.id.webview)

        myWebView.settings.javaScriptEnabled = true
        myWebView.settings.domStorageEnabled = true
        myWebView.settings.builtInZoomControls = true
        myWebView.settings.displayZoomControls = true
        myWebView.settings.useWideViewPort = true
        myWebView.settings.loadWithOverviewMode = true
        myWebView.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN


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

        binding.cameraBtn.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val photoFile = File(
                File("${filesDir}/image").apply{
                    if(!this.exists()){
                        this.mkdirs()
                    }
                },
                newJpgFileName()
            )
            photoUri = FileProvider.getUriForFile(
                this,
                "com.example.x_memory.fileprovider",
                photoFile
            )
            takePictureIntent.resolveActivity(packageManager)?.also{
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(takePictureIntent, CAMERA)
            }
        }

        binding.albumBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.setType("image/*")
            startActivityForResult(intent,GALLERY)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CAMERA -> {
                    val i = Intent(this@ProfileActivity, ConfirmActivity::class.java)
                    i.putExtra("photo", photoUri)
                    startActivity(i)
                }
                GALLERY -> {
                    var Imagedata: Uri? = data?.data
                    try {
                        val i = Intent(this@ProfileActivity, ConfirmActivity::class.java)
                        i.putExtra("album", Imagedata)
                        startActivity(i)
                    } catch (e:Exception) { e.printStackTrace() }

                }
            }
        }
    }

    private fun newJpgFileName() : String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss")
        val filename = sdf.format(System.currentTimeMillis())
        return "${filename}.jpg"
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
            R.id.menu_button -> {
                menu_button()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun logout_function(){
        val i = Intent(this@ProfileActivity, AuthActivity::class.java)
        startActivity(i)
        finish()
    }

    fun menu_button(){
        val i = Intent(this@ProfileActivity, MainActivity::class.java)
        startActivity(i)
        finish()
    }
}