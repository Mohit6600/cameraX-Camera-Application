package com.example.camera_application

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class ImageFullScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_fullscreen)


          val imageUri = intent.getStringExtra("imageUri")

      val imageView1 = findViewById<ImageView>(R.id.imageView2)
      imageView1.setImageURI(Uri.parse(imageUri))

    }
}