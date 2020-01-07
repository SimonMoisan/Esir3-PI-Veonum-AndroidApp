package com.example.industrialproject

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.vision.face.FaceDetector
import kotlin.system.exitProcess
import android.os.Looper
import android.os.Handler
import android.webkit.WebView
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T




class PrerequisiteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_prerequisite)

        val web = findViewById<WebView>(R.id.icon_view_prerequisite)
        web.setBackgroundColor(Color.TRANSPARENT)

        web.loadUrl("file:///android_asset/htmls/loading_prerequisite.html")

        val loadingThread = object : Thread() {

            override fun run() {

                try {
                    super.run()
                    testsPrerequisites()
                    sleep(1000)
                } catch (e: Exception) {
                    throw e
                } finally {
                    // If everything is okay, go automatically to the main screen
                    sleep(5000)
                    startActivityFromMainThread()
                    finish()
                }
            }
        }

        loadingThread.start()
    }

    fun startActivityFromMainThread() {

        val handler = Handler(Looper.getMainLooper())
        handler.post {
            val intent = Intent(this@PrerequisiteActivity, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateText(s: String, t: TextView){
        runOnUiThread {
            t.text = s
        }
    }

    private fun testsPrerequisites() {
        val t = findViewById<TextView>(R.id.text_view_prerequisite_details)

        // Test Google vision libs

        if(!testsGooglePrerequisites(t)){

            //If we can't use Google Vision libs
            updateText("Google Vision librairies not found !", t)
            Thread.sleep(1000)

            // Test Google Play

            if(!testsGooglePlayVersion(t)){

                //If not success reaching for Google Play Services
                updateText("Error Google Play Service unreacheable", t)
                Thread.sleep(2000)

            }else{

                //If success reaching for Google Play Services
                updateText("Google Play Service reached, please install Google Vision manually", t)
                Thread.sleep(2000)

            }

        }else{

            //If we can use Google Vision libs
            updateText("Google Vision librairies found", t)
            Thread.sleep(1000)

        }



    }

    private fun testsGooglePrerequisites(t: TextView): Boolean {

        //Wait for Face detector

        updateText("Testing Google Vision librairies ...", t)

        val faceDetector = FaceDetector.Builder(applicationContext)
            .setTrackingEnabled(false)
            .setLandmarkType(FaceDetector.ALL_LANDMARKS)
            .build()

        var faceDetectorTimeoutCounter = 0
        while(!faceDetector.isOperational && faceDetectorTimeoutCounter < 11){
           Thread.sleep(2000)
            faceDetectorTimeoutCounter += 1
            updateText("Testing Google Vision try $faceDetectorTimeoutCounter", t)
        }

        faceDetector.release()

        return faceDetector.isOperational

    }

    private fun testsGooglePlayVersion(t: TextView): Boolean {

        updateText("Testing Google Play Service availibility", t)
        val result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(applicationContext)
        return result == ConnectionResult.SUCCESS

    }

    private fun loadingTensorFlow(t: TextView): Boolean {
        return false
    }

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            finishAffinity()
        }
        exitProcess(0)
    }

}