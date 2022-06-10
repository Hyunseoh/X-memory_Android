package com.example.x_memory

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.navigation.ui.AppBarConfiguration
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.example.x_memory.databinding.ActivityProfileBinding
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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
    
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityProfileBinding
    private lateinit var myWebView: WebView
    private var image_id = ""
    // 2번 뒤로가기 변수
    var bacKeyPressedTime : Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 저장된 id,token 불러오기
        val userID = SharedPreferences.prefs.getString("id", "")
        val token = "Token " + SharedPreferences.prefs.getString("token", "")
        binding.accountName.text = userID
        val tag = ""
        var profileImage : String = "https://d1e6tpyhrf8oqe.cloudfront.net"

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
                    var filename = profile?.filename.substring(44,)
                    profileImage += filename
                    val options = RequestOptions().skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                    //프로필 이미지 등록
                    Glide.with(applicationContext).load(profileImage)
                        .apply(options)
                        .error(R.drawable.ic_account)
                        .circleCrop()
                        .into(account_iv_profile)
                }
                else {
                    Toast.makeText(applicationContext, "저장 실패", Toast.LENGTH_SHORT).show()
                }
            }
        })
        // 프로필 이미지 웹뷰 버튼






        binding.accountIvProfile.setOnClickListener {

            // Dialog만들기
            val mDialogView = LayoutInflater.from(this).inflate(R.layout.dialog, null)
            val mBuilder = AlertDialog.Builder(this)
                .setView(mDialogView)
            val mAlertDialog = mBuilder.show()

            val okButton = mDialogView.findViewById<Button>(R.id.successButton)
            okButton.setOnClickListener {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                image_id = "ok"
                intent.setType("image/*")
                startActivityForResult(intent,GALLERY)
            }

            val noButton = mDialogView.findViewById<Button>(R.id.closeButton)
            noButton.setOnClickListener {
                image_id = "ok"
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
        }



        // 웹뷰 추가
        binding.accountName.text = userID
        binding.webview.settings.javaScriptEnabled = true
        binding.webview.settings.domStorageEnabled = true
        binding.webview.settings.builtInZoomControls = true
        binding.webview.settings.displayZoomControls = true
        binding.webview.settings.useWideViewPort = true
        binding.webview.settings.loadWithOverviewMode = true
        binding.webview.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN

        val extraHeaders: MutableMap<String, String> = mutableMapOf()
        extraHeaders.put("Authorization", token)

        binding.webview?.webViewClient = object : WebViewClient() {
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
        binding.webview.loadUrl("http://xmemory.thdus.net/app_index/", extraHeaders)
        binding.webview.reload()

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
            Log.d("image",image_id)
            when (requestCode) {
                CAMERA -> {
                    val i = Intent(this@ProfileActivity, ConfirmActivity::class.java)
                    i.putExtra("photo", photoUri)
                    i.putExtra("image_id", image_id)
                    startActivity(i)
                }
                GALLERY -> {
                    var Imagedata: Uri? = data?.data
                    try {
                        val i = Intent(this@ProfileActivity, ConfirmActivity::class.java)
                        i.putExtra("album", Imagedata)
                        i.putExtra("image_id", image_id)
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
    override fun onBackPressed() {
        if(binding.webview.canGoBack()){
            binding.webview.goBack()
        }else{

            if(System.currentTimeMillis() < bacKeyPressedTime + 2500){
                finishAffinity()

                return
            }
            Toast.makeText(applicationContext, "'뒤로' 버튼을 한번 더 누르시면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show()
            bacKeyPressedTime = System.currentTimeMillis()
            //super.onBackPressed()
        }
    }


}