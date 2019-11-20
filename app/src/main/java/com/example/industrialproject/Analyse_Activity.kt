package com.example.industrialproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_analyse.*
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import android.widget.Toast
import android.graphics.drawable.BitmapDrawable
import android.graphics.RectF
import androidx.core.graphics.drawable.toBitmap
import android.util.TypedValue
import android.widget.Button
import android.widget.LinearLayout
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import java.io.IOException

class Analyse_Activity : AppCompatActivity() {

    private val PERMISSION_CODE = 1000
    private val IMAGE_CAPTURE_CODE = 1001
    var image_uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        var analyseDone = false

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analyse)
        var imageURI = intent.getStringExtra("imageUri")
        Log.d("INFO", "message : " + imageURI)
        analyse_image_view.setImageURI(Uri.parse(imageURI))

        go_back_btn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {}

            startActivity(intent)
        }

        analyse_btn.setOnClickListener {
            val image_uri: String = intent.getStringExtra("imageUri")

            if (!analyseDone) {

                try {
                    analyseImage(image_uri)
                    analyseDone = true
                }
                catch (e: Exception){
                    Log.d("ERROR", "Error : " + e.message + "\n" + e.cause)
                    Toast.makeText(this, "Error :  " + e.message, Toast.LENGTH_SHORT).show()
                }

            }
        }
    }

    fun getDipFromPixels(px: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_PX,
            px,
            Resources.getSystem().displayMetrics
        )
    }

    private fun analyseImage(imageUri:String)
    {
        //Image needs to be obtained with mutable option
        val options = BitmapFactory.Options()
        options.inMutable=true
        //val bitmapToAnalyse = BitmapFactory.decodeFile(imageUri, options)
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
        val faceDetector = FaceDetector.Builder(applicationContext)
            .setTrackingEnabled(false)
            .setLandmarkType(FaceDetector.ALL_LANDMARKS)
            .build()

        var faceDetectorTimeoutCounter = 1
        while(!faceDetector.isOperational && faceDetectorTimeoutCounter < 11){
            Toast.makeText(this,
                "Could not set up the face detector!\nTry number $faceDetectorTimeoutCounter", Toast.LENGTH_SHORT).show()
            Thread.sleep(500)
            faceDetectorTimeoutCounter += 1
        }
        if(!faceDetector.isOperational){
           throw ClassNotFoundException("FaceDetector can't work, check Google Play Service")
        }

        // Create a frame from the bitmap and detect faces
        val frame = Frame.Builder().setBitmap(bitmapToAnalyse).build()
        val faces = faceDetector.detect(frame)

        // Display rectangle for every detected face
        var faceNumberDetected = 0
        val scale = 1.0f
        for (i in 0 until faces.size())
        {
            faceNumberDetected += 1
            val thisFace:Face = faces.valueAt(i)
            val x1 = thisFace.position.x
            val y1 = thisFace.position.y
            val x2 = x1 + thisFace.width
            val y2 = y1 + thisFace.height
            tempCanvas.drawRoundRect(RectF(x1, y1, x2, y2), 40f, 40f, rectPaint)

            //Create button dynamically to be able to click on someone's face
            val dynamicButtonsLayout = findViewById<RelativeLayout>(R.id.dynamic_buttons_layout)
            val buttonDynamic = Button(this)
            // setting layout_width and layout_height using layout parameters
            val layout = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            layout.width = (thisFace.width/1.7f).toInt()
            layout.height = (thisFace.height/1.7f).toInt()

            // These holds the ratios for the ImageView and the bitmap
            val bitmapRatio  = bitmapToAnalyse.width /bitmapToAnalyse.height
            val imageViewRatio  = analyse_image_view.width /analyse_image_view.height

            val drawLeft:Int
            val drawTop:Int

            if(bitmapRatio > imageViewRatio) {
                drawLeft = 0
                val drawHeight = (imageViewRatio/bitmapRatio) * analyse_image_view.height
                drawTop = (analyse_image_view.height - drawHeight)/2
            }
            else {
                drawTop = 0;
                val drawWidth = (bitmapRatio/imageViewRatio) * analyse_image_view.width
                drawLeft = (analyse_image_view.width - drawWidth)/2
            }

            val x1dp = (x1 / this.resources.displayMetrics.density).toInt()
            val y1dp = (y1 / this.resources.displayMetrics.density).toInt()
            layout.setMargins(drawLeft,drawTop,0,0)

            buttonDynamic.layoutParams = layout
            buttonDynamic.alpha = 0.25f //transparency
            // add Button to LinearLayout
            dynamicButtonsLayout.addView(buttonDynamic)

            for (landmark in thisFace.landmarks) {
                val cx = (landmark.position.x * scale)
                val cy = (landmark.position.y * scale)
                tempCanvas.drawCircle(cx, cy, getDipFromPixels(3.0f), circlePaint)
            }
        }

        Toast.makeText(this, "$faceNumberDetected face(s) detected", Toast.LENGTH_SHORT).show()

        analyse_image_view.setImageDrawable(BitmapDrawable(resources, tempBitmap))


    }



    private fun facialReconstruction()
    {
        Toast.makeText(this, "Face reconstruction", Toast.LENGTH_SHORT).show()
    }

}
