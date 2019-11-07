package com.example.industrialproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class Analyse_Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analyse)

        go_back_btn.setOnClickListener{
            val intent = Intent(this, Analyse_Activity::class.java).apply {}
            startActivity(intent)
        }


    }
}
