package com.example.myapplication.ui

import ai.paravision.sdk.fd.BoundingBoxDetector
import ai.paravision.sdk.fd.QualityEstimator
import ai.paravision.sdk.fr.Embedding
import ai.paravision.sdk.fr.EmbeddingEstimator
import ai.paravision.sdk.fr.ScoringMode
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageInfo
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.data.Utilisateur
import com.example.myapplication.database.ApiService
import com.example.myapplication.databinding.ActivityCameraBinding
import com.example.myapplication.utilities.Statics
import com.example.myapplication.utilities.YUVConverter
import com.example.myapplication.utilities.cancelCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.util.Locale
import java.util.concurrent.Executors

class FacerecActivity : AppCompatActivity() {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 99
    }

    var liste:List<Utilisateur> = listOf<Utilisateur>()

    private lateinit var binding: ActivityCameraBinding

    private val embeddingEstimator by lazy { EmbeddingEstimator(this) }

    private val executor by lazy { Executors.newSingleThreadExecutor() }
    private val imageCapture by lazy {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    private val yuvConverter by lazy { YUVConverter(this) }

    private var takeHQPictures = false
    private var takingHQPicture = false

    private val boundingBoxDetector by lazy { BoundingBoxDetector(this) }
    private val qualityEstimator by lazy { QualityEstimator(this) }
    private var fileSaveUsr = 0
    lateinit var apiService: ApiService
    lateinit var a:String

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation =ActivityInfo.SCREEN_ORIENTATION_PORTRAIT// ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE // Landscape for RX9000 //ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        supportActionBar?.hide()
        apiService= ApiService(applicationContext)
        lifecycleScope.launch(Dispatchers.IO) {liste=apiService.rechercherUtilisateur()}
         a=resources.configuration.locales.get(0).language


        binding = ActivityCameraBinding.inflate(layoutInflater).apply {
            cameraOverlay.setZOrderOnTop(true)
            hqOn = takeHQPictures
          //  changeLanguage(Statics.currentLuanguage)


            /* buttonHQ.setOnClickListener {
                 runOnUiThread {
                     takeHQPictures = !takingHQPicture
                     hqOn = takeHQPictures
                 }
             }
             buttonClose.setOnClickListener { runOnUiThread { finish() } }*/
            if(Statics.isEnrollement==false){buttonFileSaveUsr1.setText(R.string.unerollment_user_camera)}
            else{buttonFileSaveUsr1.setText(R.string.enrollement_user_camera)}
            buttonFileSaveUsr2.setText(R.string.cancel_detection_camera)

            buttonFileSaveUsr1.setOnClickListener {
                fileSaveUsr = 1
            }
            buttonFileSaveUsr2.setOnClickListener {
                //fileSaveUsr = 2
                cancelDetect()
            }
            setContentView(root)
        }
        requestCamera()
    }

    private val permissionGranted get() = ContextCompat.checkSelfPermission(this,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestCamera() {
        if (permissionGranted) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (permissionGranted) {
                startCamera()
            } else {
                finish()
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera() {
        ProcessCameraProvider.getInstance(this).let { provider ->
            provider.addListener({
                val cameraProvider: ProcessCameraProvider = provider.get()
                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                    }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(Size(3000, 4000))
                    .build()
                    .also {
                        it.setAnalyzer(executor) { proxy ->
                            if (!takingHQPicture) {
                                processFrame(proxy.image!!, proxy.imageInfo)
                            }
                            proxy.close()
                        }
                    }
                val useCaseGroup = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(imageAnalysis)
                    .addUseCase(imageCapture)
                    .build()

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, useCaseGroup)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(this))
        }
    }


    private fun processBitmap(bitmap: Bitmap) {
        val embeddings = embeddingEstimator.getEmbeddings(bitmap)
        val embedding1 = embeddings.firstOrNull()
        /*  runOnUiThread {
              binding.debugText.text = "embeddings: ${embeddings.count()} | ${embeddings.joinToString("; ") { "vector ${it?.vector?.count()} floats, sigma ${it?.sigma}" }}\n"
          }*/

        val boxes = boundingBoxDetector.getBoundingBoxes(bitmap)
        val qualities = qualityEstimator.getQualities(bitmap, boxes)
        if(qualities.isEmpty()) {
            runOnUiThread {
                binding.debugText.setText(R.string.no_face_detected_camera)
            }
            fileSaveUsr=0
        }else if (qualities[0]?.quality!! < 0.8) {
            runOnUiThread {
                binding.debugText.setText(R.string.quality_too_low_center_your_face)
            }
            fileSaveUsr=0
        }else {
            runOnUiThread {
                binding.debugText.setText(R.string.ready_to_save_face_embedding)
            }
            if (fileSaveUsr != 0) {
                try {
                    val embData = embeddings[0]?.getData()
                    if (embData != null) {
                        // Convert FloatArray to ByteArray
                        val byteBuffer = ByteBuffer.allocate(embData.size * 4) // 4 bytes per float
                        for (value in embData) {
                            byteBuffer.putFloat(value)
                        }
                        val byteArray = byteBuffer.array()

                        //THIS IS WHERE THE EMBEDDING WILL BE RETURNED AS A BYTEARRAY
                        // Now you can store byteArray as a binary blob
                        storeAsBlob(this, byteArray, fileSaveUsr,embedding1)
                        runOnUiThread {
                            binding.debugText.setText(R.string.face_embedding_saved)
                        }
                    }
                    fileSaveUsr = 0
                } catch (e: Exception) {
                    runOnUiThread {
                        binding.debugText.setText(R.string.error_saving_face_embedding)
                    }
                }
            }
        }

    }









    fun ajouterByteAraay(imageTaken :ByteArray)
    {

        lifecycleScope.launch(Dispatchers.IO) {
            apiService.persitUtilisateur(Utilisateur(name = "youssef", image = imageTaken)) }


        val myintent = Intent(this, ResultActivity::class.java)
        myintent.putExtra("RESULT", ResultActivity.ResultType.Success)
        this.startActivity(myintent)




    }






    fun ajouterByteAraayNoSuspend(imageTaken :ByteArray)
    {

        try {
            apiService.persitUtilisateurNoSuspend(
                Utilisateur(name = LocalDateTime.now().toString(), image = imageTaken)

            )
           // Toast.makeText(this@FacerecActivity,"except", Toast.LENGTH_SHORT).show()

        }catch (e:Exception)
        { runOnUiThread {  Toast.makeText(this@FacerecActivity,e.message, Toast.LENGTH_SHORT).show()}
            return
        }


        val myintent = Intent(this, ResultActivity::class.java)
        myintent.putExtra("RESULT", ResultActivity.ResultType.Success)
        this.startActivity(myintent)

        //finish()

    }



    fun supprim(utilisateur: Utilisateur)
    {
        lifecycleScope.launch(Dispatchers.IO) { apiService.deleteUtilisateur(utilisateur)}

        val myintent = Intent(this, ResultActivity::class.java)
        myintent.putExtra("RESULT", ResultActivity.ResultType.Success)
        this.startActivity(myintent)

    }


    fun supprimerNoSuspend(utilisateur: Utilisateur)
    {

        try {
            apiService.deleteUtilisateurNoSuspend(utilisateur)


            // Toast.makeText(this@FacerecActivity,"except", Toast.LENGTH_SHORT).show()

        }catch (e:Exception)
        { runOnUiThread { Toast.makeText(this@FacerecActivity,e.message, Toast.LENGTH_SHORT).show()}
            return
        }


        val myintent = Intent(this, ResultActivity::class.java)
        myintent.putExtra("RESULT", ResultActivity.ResultType.Success)
        this.startActivity(myintent)

        //finish()

    }

    fun cancelDetect()
    {
        val myintent = Intent(this, ResultActivity::class.java);myintent.putExtra("RESULT",
        ResultActivity.ResultType.Canceled
    )
        this.startActivity(myintent)
        return
    }


    private fun changeLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

    }



    fun readEmbeddingFromByte(byteArray: ByteArray): Embedding? {
        return try {
            // Read the byte array from the file


            // Convert ByteArray back to FloatArray
            val floatBuffer = ByteBuffer.wrap(byteArray).asFloatBuffer()
            val floatArray = FloatArray(floatBuffer.remaining())
            floatBuffer.get(floatArray)

            // Gen5 (no sigma)
            Embedding(vector = floatArray)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun recognize(embedding1: Embedding?, byteArray: ByteArray?):Float
    {
        if(embedding1==null) return 0f
        if(byteArray==null) return 0f
        val embedingElment=readEmbeddingFromByte(byteArray)
        if(embedingElment==null) return 0f

        return embeddingEstimator.getSimilarity(embedding1, embedingElment, ScoringMode.STANDARD)


    }


    fun storeAsBlob(context: Context, data: ByteArray, fileSaveUsr:Int,embedding1: Embedding?) {
        // Example: Store in a file
       /* val fileName = "enrolledUserDemo" + fileSaveUsr +".bin"
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use { outputStream ->
            outputStream.write(data)
        }*/



        val u=liste.find { recognize(embedding1,it.image)> 3.0  }

        if(u!=null)
        {
            if(Statics.isEnrollement)
            {
                val myintent = Intent(this, ResultActivity::class.java)
                myintent.putExtra("RESULT", ResultActivity.ResultType.Duplicate)
                this.startActivity(myintent)
                //finish()
                return
            }else
            {
                //supprimerNoSuspend(u)
                supprim(u)
                return
            }
        }

       /* if(liste.any{recognize(embedding1,it.image)> 3.0})
        {

            val myintent = Intent(this, ResultActivity::class.java)
            myintent.putExtra("RESULT", ResultActivity.ResultType.Duplicate)
            this.startActivity(myintent)
            //finish()
            return
        }*/



        //  runOnUiThread{ Toast.makeText(this,"saving now",Toast.LENGTH_SHORT).show()}


        //lifecycleScope.launch(Dispatchers.IO){ ajouterByteAraay(data) }

        if(Statics.isEnrollement)
        {//ajouterByteAraayNoSuspend(data)
            ajouterByteAraay(data)
        }
        else
        {
            val myintent = Intent(this, ResultActivity::class.java)
            myintent.putExtra("RESULT", ResultActivity.ResultType.Unrecognized)
            this.startActivity(myintent)
        }




        /* //val baseFolder = context.getFilesDir().getAbsolutePath()  //TEST
         val fileName = "/data/local/enrolledUserDemo"+ fileSaveUsr +".bin"
         val file = File(fileName)
         //file.getParentFile()?.mkdirs();
         val fos  = FileOutputStream(file);
         fos.write(data);
         fos.flush();
         fos.close();*/
    }

    private fun processFrame(image: Image, info: ImageInfo) {
        imageToBitmap(image)?.let {
            val bitmap = rotateBitmap(it, info.rotationDegrees)
            processBitmap(bitmap)
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int, mirror: Boolean = true): Bitmap {
        val matrix = Matrix().also {
            it.postRotate(rotationDegrees.toFloat())
            if (mirror) it.postScale(-1.0f, 1.0f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    @Synchronized
    private fun imageToBitmap(image: Image): Bitmap? {
        if (image.format == ImageFormat.JPEG) {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } else if (image.format == ImageFormat.YUV_420_888) {
            val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
            yuvConverter.yuvToRgb(image, bitmap)
            return bitmap
        }
        println("Unsupported image format: ${image.format}")
        return null
    }
}