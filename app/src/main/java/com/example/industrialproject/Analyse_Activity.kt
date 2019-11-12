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
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T




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
            val image_uri:String = intent.getStringExtra("imageUri")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                {
                    //permission was not enabled
                    val permission = arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    //show popup to request permission
                    requestPermissions(permission, PERMISSION_CODE)
                }
                else
                {
                    //permission already granted
                    analyseImage(image_uri)
                }
            }
            else
            {
                //system os is < marshmallow
                analyseImage(image_uri)
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
        if (!faceDetector.isOperational()) {
            Toast.makeText(this, "Could not set up the face detector!", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a frame from the bitmap and detect faces
        val frame = Frame.Builder().setBitmap(bitmapToAnalyse).build()
        val faces = faceDetector.detect(frame)

        // Display rectangle for every detected face
        val scale = 1.0f
        for (i in 0 until faces.size()) {
            val thisFace:Face = faces.valueAt(i)
            val x1 = thisFace.position.x
            val y1 = thisFace.position.y
            val x2 = x1 + thisFace.width
            val y2 = y1 + thisFace.height
            tempCanvas.drawRoundRect(RectF(x1, y1, x2, y2), 2f, 2f, rectPaint)

            for (landmark in thisFace.landmarks) {
                val cx = (landmark.position.x * scale)
                val cy = (landmark.position.y * scale)
                tempCanvas.drawCircle(cx, cy, getDipFromPixels(3.0f), circlePaint)
            }

        }
        analyse_image_view.setImageDrawable(BitmapDrawable(resources, tempBitmap))
    }

}
