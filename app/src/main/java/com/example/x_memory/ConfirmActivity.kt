package com.example.x_memory

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.example.x_memory.databinding.ActivityConfirmBinding
import java.io.*


private lateinit var binding: ActivityConfirmBinding

class ConfirmActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityConfirmBinding.inflate(layoutInflater)
        setContentView(binding.root)
        var photoUri : Uri? = null
        lateinit var filename_photo : String

        var Imagedata : Uri? = null
        lateinit var filename_album : String
        val userID = SharedPreferences.prefs.getString("id", "")

        try {
            photoUri= getIntent().getParcelableExtra("photo")
            val imageBitmap = photoUri?.let { ImageDecoder.createSource(this.contentResolver, it) }
            binding.confirm.setImageBitmap(imageBitmap?.let { ImageDecoder.decodeBitmap(it) })
            filename_photo = "/" + userID + photoUri?.lastPathSegment

//            Toast.makeText(this, photoUri?.path, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            Imagedata = getIntent().getParcelableExtra("album")
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Imagedata)
            binding.confirm.setImageBitmap(bitmap)
            filename_album = "/" + userID + Imagedata?.lastPathSegment + ".jpg"

        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding.downloadButton.setOnClickListener {

            val photosteram = photoUri?.let { contentResolver.openInputStream(it) }
            if (photosteram != null) {
                uploadWithTransferUtility(filename_photo,photosteram)
            }
            val imagesteram = Imagedata?.let { contentResolver.openInputStream(it) }
            if (imagesteram != null) {
                uploadWithTransferUtility(filename_album,imagesteram)
            }
        }

    }

    fun uploadWithTransferUtility(fileName: String, file: InputStream) {

        val credentialsProvider = CognitoCachingCredentialsProvider(
            applicationContext,
            "us-east-2:0f532ce8-1def-48c4-b741-c768ffaf85b4", // 자격 증명 풀 ID
            Regions.US_EAST_2 // 리전
        )

        TransferNetworkLossHandler.getInstance(applicationContext)

        val AWS_STORAGE_BUCKET_NAME = "cloud01-2"
        val transferUtility = TransferUtility.builder()
            .context(applicationContext)
            .defaultBucket(AWS_STORAGE_BUCKET_NAME)
            .s3Client(AmazonS3Client(credentialsProvider, Region.getRegion(Regions.US_EAST_2)))
            .build()

        /* Store the new created Image file path */

        val uploadObserver = transferUtility.upload("public${fileName}", file)

        //CannedAccessControlList.PublicRead 읽기 권한 추가

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
            }

            override fun onError(id: Int, ex: Exception) {
                Log.d("MYTAG", "UPLOAD ERROR - - ID: $id - - EX: ${ex.message.toString()}")
            }
        })

        // If you prefer to long-poll for updates
        if (uploadObserver.state == TransferState.COMPLETED) {
            /* Handle completion */

        }
    }


}


//경로
//            Toast.makeText(this, Imagedata?.path, Toast.LENGTH_LONG).show()

//위도, 경도
//            var path = createCopyAndReturnRealPath(Imagedata!!)
//            val exif = ExifInterface(path!!)
//            binding.latitude.text = exif.latLong?.get(0).toString()
//            binding.longitude.text = exif.latLong?.get(1).toString()

