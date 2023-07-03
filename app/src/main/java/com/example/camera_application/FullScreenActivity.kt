package com.example.camera_application

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class FullScreenActivity : AppCompatActivity() {

    lateinit var mediaController: MediaController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen)

        mediaController = MediaController(this)


        val videoUri = intent.getStringExtra("videoUri")

        val videoView = findViewById<VideoView>(R.id.videoView1)
        videoView.setVideoURI(Uri.parse(videoUri))
        videoView.setMediaController(mediaController)
        videoView.start()


      /*  val imageUri = intent.getStringExtra("imageUri")

        val imageView1 = findViewById<ImageView>(R.id.imageView2)
        imageView1.setImageURI(Uri.parse(imageUri))*/
    }
}
// i have to check why camera image not go to full screen activity
