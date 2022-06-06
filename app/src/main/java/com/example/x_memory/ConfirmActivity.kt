package com.example.x_memory

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.example.x_memory.databinding.ActivityConfirmBinding
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.*
import java.net.CookieManager


private lateinit var binding: ActivityConfirmBinding
private lateinit var pathstream : InputStream
private lateinit var filename : String

class ConfirmActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityConfirmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var photoUri : Uri?
        lateinit var filename_photo : String
        var Imagedata: Uri?
        val userID = SharedPreferences.prefs.getString("id", "")

        val client = OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(CookieManager())) //쿠키매니저 연결
            .build()

        var retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl("http://xmemory.thdus.net")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        var uploadService: UploadService = retrofit.create(UploadService::class.java)

        val locationmanager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            val isGPSEnabled : Boolean = locationmanager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled : Boolean = locationmanager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@ConfirmActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
            } else {

                photoUri = getIntent().getParcelableExtra("photo")
                val imageBitmap =
                    photoUri?.let { ImageDecoder.createSource(this.contentResolver, it) }
                binding.confirm.setImageBitmap(imageBitmap?.let { ImageDecoder.decodeBitmap(it) })
                // 카메라 사진 경로, InputStream 변환
                filename_photo = "/" + userID + "/" + photoUri?.lastPathSegment.toString()
                pathstream = photoUri?.let { contentResolver.openInputStream(it) }!!
                filename = photoUri?.lastPathSegment.toString()

                when {
                    isNetworkEnabled -> {
                        val location =
                            locationmanager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) //인터넷기반으로 위치를 찾음

                        // 사진 찍었을 때의 위치, 경도 (사진에 포함하는 걸 모르겠어서 찍었을 때의 정보를 가져옴)
                        binding.latitude.text = location?.latitude.toString()
                        binding.longitude.text = location?.longitude.toString()

                        // 시간 정보 불러오기
                        var path = createCopyAndReturnRealPath(photoUri!!)
                        val exif = ExifInterface(path!!)
                        binding.datetime.text = exif.getAttribute(ExifInterface.TAG_DATETIME)
                    }
                }
            }


//            Toast.makeText(this, photoUri?.path, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            Imagedata = getIntent().getParcelableExtra("album")
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Imagedata)
            binding.confirm.setImageBitmap(bitmap)
            // 앨범 사진 경로, InputStream 변환
            filename_photo = "/" + userID + "/" + Imagedata?.lastPathSegment + ".jpg"
            pathstream = Imagedata?.let { contentResolver.openInputStream(it) }!!
            filename = Imagedata?.lastPathSegment + ".jpg"

            var path = createCopyAndReturnRealPath(Imagedata!!)
            val exif = ExifInterface(path!!)

            // 앨범 불러왔을 때의 위치, 경도, 시간 (사진에 저장된 정보)
            binding.latitude.text = exif.latLong?.get(0).toString()
            binding.longitude.text = exif.latLong?.get(1).toString()
            binding.datetime.text = exif.getAttribute(ExifInterface.TAG_DATETIME)

//            https://cloud01-2.s3.us-east-2.amazonaws.com/public/hyunjin/image:24979.jpg

        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding.downloadButton.setOnClickListener {
            uploadWithTransferUtility(filename_photo,pathstream)
            val token = "Token " + SharedPreferences.prefs.getString("token", "")
            uploadService.requestUpload(token, filename).enqueue(object: Callback<Upload> {
                override fun onFailure(call: Call<Upload>, t: Throwable) {

                    var dialog = AlertDialog.Builder(this@ConfirmActivity)
                    dialog.setTitle("에러")
                    dialog.setMessage("호출실패했습니다.")
                    dialog.show()
                }

                override fun onResponse(call: Call<Upload>, response: Response<Upload>) {
                    var upload = response.body()
                    Log.d("upload","code : "+upload?.code)
                    if( upload?.code == "200") {
                        Toast.makeText(applicationContext, "저장됐습니다!", Toast.LENGTH_SHORT).show()
                        val i = Intent(this@ConfirmActivity, ProfileActivity::class.java)
                        startActivity(i)
                        finish()
                    }
                    else {
                        Toast.makeText(applicationContext, "저장 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }

    }

    fun createCopyAndReturnRealPath(uri: Uri) :String? {
        val context = applicationContext
        val contentResolver = context.contentResolver ?: return null

        // Create file path inside app's data dir
        val filePath = (context.applicationInfo.dataDir + File.separator
                + System.currentTimeMillis())
        val file = File(filePath)
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val outputStream: OutputStream = FileOutputStream(file)
            val buf = ByteArray(1024)
            var len: Int
            while (inputStream.read(buf).also { len = it } > 0) outputStream.write(buf, 0, len)
            outputStream.close()
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            /*  절대 경로를 getGps()에 넘겨주기   */
//            getGps(file.getAbsolutePath())
        }
        return file.getAbsolutePath()
    }

    fun uploadWithTransferUtility(fileName: String, file: InputStream) {

        val CognitoPoolId = getString(R.string.CognitoPoolId)
        val BucketName = getString(R.string.BucketName)

        val credentialsProvider = CognitoCachingCredentialsProvider(
            applicationContext,
            CognitoPoolId, // 자격 증명 풀 ID
            Regions.US_EAST_2 // 리전
        )

        TransferNetworkLossHandler.getInstance(applicationContext)

        val AWS_STORAGE_BUCKET_NAME = BucketName
        val transferUtility = TransferUtility.builder()
            .context(applicationContext)
            .defaultBucket(AWS_STORAGE_BUCKET_NAME)
            .s3Client(AmazonS3Client(credentialsProvider, Region.getRegion(Regions.US_EAST_2)))
            .build()

        val uploadObserver = transferUtility.upload("public${fileName}", file)

        // Attach a listener to the observer
        uploadObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState) {
                if (state == TransferState.COMPLETED) {
                    // Handle a completed upload
                }
            }

            override fun onProgressChanged(id: Int, current: Long, total: Long) {
                val done = (((current.toDouble() / total) * 100.0).toInt())
                Log.d("MYTAG", "UPLOAD - - ID: $id, percent done = $done")
                Toast.makeText(this@ConfirmActivity, "파일을 성공적으로 업로드했습니다.", Toast.LENGTH_SHORT).show()
            }

            override fun onError(id: Int, ex: Exception) {
                Log.d("MYTAG", "UPLOAD ERROR - - ID: $id - - EX: ${ex.message.toString()}")
            }
        })
    }

}


//경로
//            Toast.makeText(this, Imagedata?.path, Toast.LENGTH_LONG).show()

//위도, 경도
//            var path = createCopyAndReturnRealPath(Imagedata!!)
//            val exif = ExifInterface(path!!)
//            binding.latitude.text = exif.latLong?.get(0).toString()
//            binding.longitude.text = exif.latLong?.get(1).toString()

