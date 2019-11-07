package com.example.industrialproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_analyse.*
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.net.Uri
import android.util.Log


class Analyse_Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analyse)
        var imageURI=intent.getStringExtra("imageUri")
        Log.d("INFO", "message : " + imageURI)
        image_view_before_analyse.setImageURI(Uri.parse(imageURI))

        go_back_btn.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java).apply {}

            startActivity(intent)
        }


    }
}
