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
                } catch (e: Exception) {

                } finally {
                    // If everything is okay, go automatically to the main screen
                    Thread.sleep(5000)
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

    private fun testsPrerequisites() {
        val t = findViewById<TextView>(R.id.text_view_prerequisite_details)

        // Test Google vision libs
        if(!testsGooglePrerequisites(t)){
            //If we can't use Google Vision libs

            // Test Google Play

            if(!testsGooglePlayVersion(t)){

                //If not success reaching for Google Play Services

            }else{

                //If success reaching for Google Play Services

            }

        }else{

            //If we can use Google Vision libs

        }



    }

    private fun testsGooglePrerequisites(t: TextView): Boolean {

        //Wait for Face detector


        t.text = "Testing Google Vision librairies ..."

        val faceDetector = FaceDetector.Builder(applicationContext)
            .setTrackingEnabled(false)
            .setLandmarkType(FaceDetector.ALL_LANDMARKS)
            .build()

        var faceDetectorTimeoutCounter = 0
        while(!faceDetector.isOperational && faceDetectorTimeoutCounter < 11){
            Thread.sleep(2000)
            faceDetectorTimeoutCounter += 1
            t.text = "Testing Google Vision librairies $faceDetectorTimeoutCounter try"
        }

        return if(!faceDetector.isOperational){
            faceDetector.release()
            t.text = "Google Vision librairies not found !"
            false
        }else {
            faceDetector.release()
            t.text = "Google Vision librairies found"
            true
        }
    }

    private fun testsGooglePlayVersion(t: TextView): Boolean {

        t.text = "Testing Google Play Service availibility"
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