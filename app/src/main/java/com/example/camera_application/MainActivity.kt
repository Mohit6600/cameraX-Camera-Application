package com.example.camera_application

import android.content.pm.PackageManager
import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Display
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView
import androidx.core.content.ContentProviderCompat.requireContext
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewFinder: PreviewView
    private lateinit var toggleFlashlight: ImageButton
    private var clickCount = 1
    var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var savedUri: Uri
    lateinit var switchCameraBtn: ImageButton
    private val pickImage = 100
    private var imageUri: Uri? = null
    lateinit var chooseImage: ImageButton




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Hide the action bar
        supportActionBar?.hide()

        viewFinder = findViewById(R.id.viewFinder)
        toggleFlashlight = findViewById(R.id.toggleFlashlight)
        switchCameraBtn = findViewById(R.id.changeCameraButton)
        chooseImage = findViewById(R.id.chooseImage)
        val videoCam = findViewById<ImageButton>(R.id.videoCam)

        if (allPermissionsGranted()) {
            startCamera(cameraSelector)
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSION, REQUEST_CODE_PERMISSION)
        }

        findViewById<Button>(R.id.captureButton).setOnClickListener {
            takePhoto()
        }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        switchCamera()

        chooseImage.setOnClickListener {

            //this code is used for to acces the gallary of the user to see a photo

         /*   val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)

            startActivityForResult(gallery, pickImage)*/

            // it is used to see the last photo only
            val intent = Intent(this, ImageFullScreenActivity::class.java)

            intent.putExtra("imageUri", savedUri.toString())
            startActivity(intent)


        }

        videoCam.setOnClickListener {

            val intent = Intent(this, VideoRecorder::class.java)
            startActivity(intent)

        }




    }

    private fun takePhoto() {

        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOption,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "photo capture failed : ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                     savedUri = Uri.fromFile(photoFile)

                    findViewById<ImageButton>(R.id.chooseImage).visibility = View.VISIBLE
                    findViewById<ImageButton>(R.id.chooseImage).setImageURI(savedUri)

                    val msg = "photo capture succeded : $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                    Log.d(TAG, msg)
                }

            }
        )

    }

    private fun startCamera(cameraSelector: CameraSelector) {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()



            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(Tag, "Use case binding failed", exc)
            }

            val camera =
                cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            val hasFlashUnit = camera?.cameraInfo?.hasFlashUnit()

// this code is used for flash light option
            toggleFlashlight.setOnClickListener {

                if (hasFlashUnit == true && clickCount == 1) {

                    camera.cameraControl.enableTorch(true)
                    clickCount = 0

                } else {
                    camera?.cameraControl?.enableTorch(false)
                    clickCount = 1

                }

            }
// it is used for zooming the camera    from there::
            val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener(){
                override fun onScale(detector: ScaleGestureDetector): Boolean {

                    val scale = camera!!.cameraInfo.zoomState.value!!.zoomRatio * detector.scaleFactor
                    camera!!.cameraControl.setZoomRatio(scale)
                    return true
                }
            }

            val scaleGestureDetector = ScaleGestureDetector(this,listener)

            viewFinder.setOnTouchListener { _, event ->
                scaleGestureDetector.onTouchEvent(event)
                return@setOnTouchListener true
            }

            // :: from here

        }, ContextCompat.getMainExecutor(this))

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSION.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {

        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdir() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (allPermissionsGranted()) {
                startCamera(cameraSelector)
            } else {
                Toast.makeText(this, "Permission not granted by the user", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    companion object {
        private const val Tag = "camera"
        private const val REQUEST_CODE_PERMISSION = 20
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun switchCamera() {

        switchCameraBtn.setOnClickListener {

            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {

                CameraSelector.DEFAULT_FRONT_CAMERA

            } else {

                CameraSelector.DEFAULT_BACK_CAMERA

            }

            startCamera(cameraSelector)

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            imageUri = data?.data
            /* imageView.setImageURI(imageUri)*/     // it used to show the image above preview screen

//   //this code is used for to acces the gallary of the user to see a photo

       /*     val intent = Intent(this, ImageFullScreenActivity::class.java)

            intent.putExtra("imageUri", imageUri.toString())
            startActivity(intent)
*/
        }

    }
}
