package com.example.industrialproject

import android.content.Intent
import android.content.res.Resources
import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_analyse.*
import android.net.Uri
import android.util.Log
import android.media.FaceDetector.Face
import android.media.FaceDetector
import android.graphics.drawable.BitmapDrawable
import android.graphics.RectF
import androidx.core.graphics.drawable.toBitmap
import android.util.TypedValue
import android.graphics.PointF





class Analyse_Activity : AppCompatActivity() {

    private val PERMISSION_CODE = 1000;
    private val IMAGE_CAPTURE_CODE = 1001
    var image_uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analyse)
        var imageURI=intent.getStringExtra("imageUri")
        Log.d("INFO", "message : " + imageURI)
        analyse_image_view.setImageURI(Uri.parse(imageURI))

        go_back_btn.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java).apply {}

            startActivity(intent)
        }

        analyse_btn.setOnClickListener{
            analyseImage()
        }
    }

    private fun getDipFromPixels(px: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_PX,
            px,
            Resources.getSystem().displayMetrics
        )
    }

    private fun analyseImage()
    {
        //Image needs to be obtained with mutable option
        val options = BitmapFactory.Options()
        options.inMutable=true
        val bitmapToAnalyse = analyse_image_view.drawable.toBitmap()
        
        //Paint object to display red squares for the faces
        val rectPaint = Paint()
        rectPaint.strokeWidth = getDipFromPixels(5.0f)
        rectPaint.color = Color.RED
        rectPaint.style = Paint.Style.STROKE

        //Paint object to display green circles for the face features
        val circlePaint = Paint()
        circlePaint.strokeWidth = getDipFromPixels(2.0f)
        circlePaint.color = Color.GREEN
        circlePaint.style = Paint.Style.STROKE

        //Temp bitmap and canvas to draw on
        val tempBitmap = Bitmap.createBitmap(bitmapToAnalyse.width, bitmapToAnalyse.height, Bitmap.Config.RGB_565)
        val tempCanvas = Canvas(tempBitmap)
        tempCanvas.drawBitmap(bitmapToAnalyse, 0.0f, 0.0f, null)

        //Face detector
        val maxFaces = 20
        val myFaceDetector = FaceDetector(tempBitmap.width, tempBitmap.height, maxFaces)
        val detectedFaces = arrayOfNulls<Face?>(maxFaces)

        val nbFaceDetected = myFaceDetector.findFaces(tempBitmap, detectedFaces)
        // Display rectangle for every detected face
        val scale = 1.0f
        for (i in 0 until nbFaceDetected) {

            val thisFace:Face? = detectedFaces[i]
            if(thisFace != null){

                val midPoint = PointF()
                thisFace.getMidPoint(midPoint)
                val eyeDistance = thisFace.eyesDistance()

                val left = (midPoint.x - (1.4 * eyeDistance) as Float)
                val top = (midPoint.y - (1.8 * eyeDistance) as Float)
                val right = (midPoint.x + (1.4 * eyeDistance) as Float)
                val down = (midPoint.y + (1.8 * eyeDistance) as Float)

                tempCanvas.drawRoundRect(RectF(left, top, right, down), 2f, 2f, rectPaint)

            }
        }
        analyse_image_view.setImageDrawable(BitmapDrawable(resources, tempBitmap))
    }

}
