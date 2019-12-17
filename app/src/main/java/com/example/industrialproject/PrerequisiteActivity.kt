package com.example.industrialproject

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class PrerequisiteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_analyse)



    }
}