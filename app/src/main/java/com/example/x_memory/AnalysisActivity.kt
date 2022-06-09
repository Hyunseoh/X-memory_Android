package com.example.x_memory

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.ui.AppBarConfiguration
import com.example.x_memory.databinding.ActivityAnalysisBinding
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager


class AnalysisActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityAnalysisBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val token = "Token " + SharedPreferences.prefs.getString("token", "")

        binding.analysisView.settings.javaScriptEnabled = true
        binding.analysisView.settings.domStorageEnabled = true
        binding.analysisView.settings.builtInZoomControls = true
        binding.analysisView.settings.displayZoomControls = true
        binding.analysisView.settings.useWideViewPort = true
        binding.analysisView.settings.loadWithOverviewMode = true
        binding.analysisView.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN

        val extraHeaders: MutableMap<String, String> = mutableMapOf()
        extraHeaders.put("Authorization", token)

        binding.analysisView.webViewClient = object : WebViewClient() {
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
        binding.analysisView.loadUrl("http://xmemory.thdus.net/app_analysis/", extraHeaders)
        binding.analysisView.reload()
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
        val i = Intent(this@AnalysisActivity, AuthActivity::class.java)
        startActivity(i)
        finish()
    }

    fun menu_button(){
        val i = Intent(this@AnalysisActivity, MainActivity::class.java)
        startActivity(i)
        finish()
    }

    override fun onBackPressed() {
        if(binding.analysisView.canGoBack()){
            binding.analysisView.goBack()
        }else{
            super.onBackPressed()
        }
    }
}