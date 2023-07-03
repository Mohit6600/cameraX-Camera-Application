package com.example.camera_application

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.camera.core.CameraSelector
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.camera_application.databinding.ActivityVideoRecorderBinding
import java.lang.Exception
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class VideoRecorder : AppCompatActivity() {

    private lateinit var viewBinding: ActivityVideoRecorderBinding
    private var videoCapture: androidx.camera.video.VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService
    lateinit var viewFinder: androidx.camera.view.PreviewView
    var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    var clickCount = 1
    private val pickImage = 100
    private var videoUri: Uri? = null
    private var timer: Timer? = null
    private var recorderSecondsElapsed=0


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityVideoRecorderBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewFinder = findViewById(R.id.viewFinder)

        if (allPermissionGranted()) {
            startCamera(cameraSelector)
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSION)
        }
        viewBinding.videoCaptureBtn.setOnClickListener {
            captureVideo()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        viewBinding.chooseImage2.setOnClickListener {

            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.INTERNAL_CONTENT_URI)

            startActivityForResult(gallery, pickImage)

        }

        viewBinding.cameraCam.setOnClickListener {

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


        switchCamera2()


    }

    private fun captureVideo() {

        val videoCapture = this.videoCapture ?: return
        viewBinding.videoCaptureBtn.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            curRecording.stop()
            recording = null
            stopTimer()
            resetTimer()
            return
        }

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }
        val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        recording = videoCapture.output.prepareRecording(this, mediaStoreOutputOptions).apply {

            if (PermissionChecker.checkSelfPermission(
                    this@VideoRecorder,
                    Manifest.permission.RECORD_AUDIO
                ) == PermissionChecker.PERMISSION_GRANTED
            ) {
                withAudioEnabled()
            }
        }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                //lambda event listener
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        viewBinding.videoCaptureBtn.apply {
                            text = getString(R.string.stop_capture)
                            isEnabled = true
                        }
                        startTimer()
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg =
                                "Video capture succeeded: " + "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                            Log.d(TAG, msg)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: " + "${recordEvent.error}")
                        }
                        viewBinding.videoCaptureBtn.apply {
                            text = getString(R.string.start_capture)
                            isEnabled = true
                        }
                        stopTimer()
                    }
                }
            }
    }


    private fun startCamera(cameraSelector: CameraSelector) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            val recorder =
                Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build()

            videoCapture = androidx.camera.video.VideoCapture.withOutput(recorder)

            /*     val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA*/

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
            } catch (exc: Exception) {

                Log.e(TAG, "Use case binding failed", exc)
            }

// it is used for flash light start
            val camera =
                cameraProvider?.bindToLifecycle(this, cameraSelector, preview, videoCapture)
            val hasFlashUnit = camera?.cameraInfo?.hasFlashUnit()

            viewBinding.toggleFlashlight2.setOnClickListener {

                if (hasFlashUnit == true && clickCount == 1) {

                    camera.cameraControl.enableTorch(true)
                    clickCount = 0

                } else {
                    camera?.cameraControl?.enableTorch(false)
                    clickCount = 1

                }

            }

            // end the above code used for flash light

        }, ContextCompat.getMainExecutor(this))


    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (allPermissionGranted()) {
                startCamera(cameraSelector)
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_LONG)
                    .show()
                finish()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {

        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT =
            "yyyy-MM-dd-HH-mm-ss-SSS" // how to save a image or viedeo like year moth etc.
        private const val REQUEST_CODE_PERMISSION = 10

        @RequiresApi(Build.VERSION_CODES.Q)
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    private fun switchCamera2() {

        viewBinding.changeCameraButton2.setOnClickListener {

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
            videoUri = data?.data
            /* imageView.setImageURI(imageUri)*/     // it used to show the image above preview screen


            val intent = Intent(this, FullScreenActivity::class.java)

            intent.putExtra("videoUri", videoUri.toString())
            startActivity(intent)

        }

    }

    private fun startTimer() {
        stopTimer()
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask(){
            override fun run() {
               /*viewBinding.timerTxt.visibility= View.GONE*/
                updateTimer()
            }

        },0,1000)
        }

    private fun stopTimer(){
        if(timer != null){
            timer?.cancel()
            timer?.purge()
            timer = null
        }
    }

    private fun updateTimer() {
        Handler(Looper.getMainLooper()).post {
            val minutes = recorderSecondsElapsed / 60
            val seconds = recorderSecondsElapsed % 60
            val timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            viewBinding.timerTxt.text = timeString
            recorderSecondsElapsed++
        }
    }

    private fun resetTimer(){

        recorderSecondsElapsed = 0
        updateTimer()

    }

    }


