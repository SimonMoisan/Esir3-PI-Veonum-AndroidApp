package com.example.industrialproject

import android.content.Intent
import android.content.res.Resources
import android.graphics.*
import android.graphics.Bitmap.createBitmap
import android.graphics.Bitmap.createScaledBitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_analyse.*
import android.net.Uri
import android.util.Log
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import android.graphics.drawable.BitmapDrawable
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import androidx.core.graphics.drawable.toBitmap
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.view.setMargins
import com.example.industrialproject.saveImageToExternalStorage

class AnalyseActivity : AppCompatActivity() {

    //Indicate if a button feature is already active on the image
    var buttonFeatureIsActive = false

    var viewDialog: AnalyseLoadingPopup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        var analyseDone = false

        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_analyse)
        var imageURI = intent.getStringExtra("imageUri")
        Log.d("INFO", "message : $imageURI")
        analyse_image_view.setImageURI(Uri.parse(imageURI))

        val imageUri: String = intent.getStringExtra("imageUri")


        val analyseThread = object : Thread() {
            override fun run() {
                try {
                    super.run()
                    analyseImage(imageUri)
                    analyseDone = true
                } catch (e: Exception) {
                    Log.d("ERROR", "Error in analyse thread execution")
                    e.printStackTrace()
                    throw e
                } finally {

                }
            }
        }

        // Set the touch listener on the "go back" button. Make the app return to the main activity when clicked
        go_back_btn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {}
            startActivity(intent)
        }

        // Set the touch listener on the "Save" button. Make the app save the current image in the device memory.
        save_image_btn.setOnClickListener {
            try {
                saveImageToExternalStorage(analyse_image_view.drawable.toBitmap(), this)
                toastOnMainThread("New image saved")
            } catch (e :Exception){
                toastOnMainThread("Error saving image")
            }
        }

        // Set the touch listener on the "Analyse" button. Make the app launch the analysis of the image by the Google mobile Vision librairies
        // Launched with another thread, to prevent freeze of the main (UI) thread, the analyse can take some time with big images.
        analyse_btn.setOnClickListener {
            if (!analyseDone) {
                analyseThread.start()
            }
        }

        // We initialize the object that stock the loading gif with the current context, for later use
        viewDialog = AnalyseLoadingPopup(this)
    }

    //Function that permit threads different that the main (UI) thread to make toasts (simples text messages)
    private fun toastOnMainThread(message : String) {
        // This handler force the threads to let the main thread execute the code inside the post()
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
        // Handler to execute UI code on the UI thread (main thread)
        val handler = Handler(Looper.getMainLooper())

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
        handler.post {
            tempCanvas.drawBitmap(bitmapToAnalyse, 0.0f, 0.0f, null)
        }

        //Face detector
        val faceDetector = FaceDetector.Builder(applicationContext)
            .setTrackingEnabled(false)
            .setLandmarkType(FaceDetector.ALL_LANDMARKS)
            .build()

        if(!faceDetector.isOperational){
            faceDetector.release()
            throw ClassNotFoundException("FaceDetector can't work, check Google Play Service")
        }

        // Create a frame from the bitmap and detect faces
        val frame = Frame.Builder().setBitmap(bitmapToAnalyse).build()

        handler.post{
            viewDialog!!.showDialog()
        }

        val faces = faceDetector.detect(frame)

        faceDetector.release()
        handler.post{
            viewDialog!!.hideDialog()
        }
        
        // Display rectangle for every detected face
        var faceNumberDetected = 0
        val scale = 1.0f

        //Create face selection buttons
        var listOfButtons: MutableList<Button> = mutableListOf()
        var listOffFaceValues: MutableList<List<Float>> = mutableListOf()

        for (i in 0 until faces.size())
        {
            faceNumberDetected += 1
            val thisFace:Face = faces.valueAt(i)
            val x1 = thisFace.position.x
            val y1 = thisFace.position.y
            val x2 = x1 + thisFace.width
            val y2 = y1 + thisFace.height
            handler.post {
                //tempCanvas.drawRoundRect(RectF(x1, y1, x2, y2), 10f, 10f, rectPaint)
            }

            //Create button dynamically to be able to click on someone's face
            val dynamicButtonsLayout = findViewById<FrameLayout>(R.id.dynamic_buttons_layout)
            val buttonDynamicFace = Button(this)
            buttonDynamicFace.setBackgroundResource(R.drawable.buttonanalyse_face)

            // setting layout_width and layout_height using layout parameters
            val layout = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)

            val heightRatio = analyse_image_view.height / bitmapToAnalyse.height.toDouble()
            val widthRatio = analyse_image_view.width / bitmapToAnalyse.width.toDouble()

            var biggestRatio:Double = widthRatio

            if (heightRatio < widthRatio && heightRatio < 1) {
                biggestRatio = heightRatio
            }

            var dynamicLayoutX = analyse_image_view.left + (x1 * biggestRatio).toInt()
            var dynamicLayoutY = analyse_image_view.top + (y1 * biggestRatio).toInt()

            layout.width = (thisFace.width * biggestRatio).toInt()
            layout.height = (thisFace.height * biggestRatio).toInt()

            layout.setMargins(dynamicLayoutX, dynamicLayoutY,0,0)

            buttonDynamicFace.layoutParams = layout

            // add Button to layout and to the list
            handler.post {
                dynamicButtonsLayout.addView(buttonDynamicFace)
            }
            listOfButtons.add(buttonDynamicFace)

            // add values (x, y, width and height) to a list for later use
            val tempList = listOf(x1, y1, x2, y2)
            listOffFaceValues.add(tempList)

            //Add landmark on eyes, mouth, etc...
            handler.post {
                for (landmark in thisFace.landmarks) {
                    val cx = (landmark.position.x * scale)
                    val cy = (landmark.position.y * scale)
                    //tempCanvas.drawCircle(cx, cy, getDipFromPixels(3.0f), circlePaint)
                }
            }
        }

        //Set listener for every buttons created
        var faceFeatureButton = Button(this)

        for (i in 0 until listOfButtons.size) {
            var button = listOfButtons[i]

            //Add button onClickListener
            button.setOnClickListener(View.OnClickListener {
                //Add face option buttons
                var currentFace = faces.valueAt(i)
                if (!buttonFeatureIsActive) //if no button activate
                {

                    faceFeatureButton = displayFacialFeatureButtons(button, currentFace)
                    buttonFeatureIsActive = true
                } else //Remove other regeneration button
                {
                    val dynamicButtonsLayout =
                        findViewById<FrameLayout>(R.id.dynamic_buttons_layout)
                    dynamicButtonsLayout.removeView(faceFeatureButton)
                    faceFeatureButton = displayFacialFeatureButtons(button, currentFace)
                }
            })
        }

        toastOnMainThread("$faceNumberDetected face(s) detected")
        handler.post {
            analyse_image_view.setImageDrawable(BitmapDrawable(resources, tempBitmap))
        }
    }

    private fun displayFacialFeatureButtons(parentButton:Button, face:Face) : Button
    {
        val dynamicButtonsLayout = findViewById<FrameLayout>(R.id.dynamic_buttons_layout)

        val buttonFacialFeature = Button(this)

        val bounds = Rect()
        val textView = TextView(this)
        val buttonText = "Regeneration"

        val activityTextSize = findViewById<Button>(R.id.go_back_btn).textSize / resources.displayMetrics.scaledDensity
        textView.textSize = activityTextSize
        textView.paint.getTextBounds(buttonText, 0, buttonText.length, bounds)
        val textSizeW = bounds.width()
        val textSizeH = bounds.height()

        val layout = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)

        //Check if the button is outside of the screen
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val screenWidth = size.x
        val parentButtonWidthPx = parentButton.width
        val parentButtonHeightPx = parentButton.height

        val xOffset = 100

        //TODO The others possibilities

        if(parentButton.x + textSizeW + parentButtonWidthPx + xOffset >= screenWidth &&
                parentButton.x - textSizeW - parentButton.width <= 0){

            val decal1 = textSizeW/2
            val decal2 = (parentButtonWidthPx / resources.displayMetrics.density) / 2
            val decal = decal1 - decal2

            layout.setMargins(parentButton.left - decal.toInt(), parentButton.top + parentButtonHeightPx,0,0)
        }
        else if(parentButton.x + textSizeW + parentButtonWidthPx + xOffset >= screenWidth) {
            layout.setMargins(parentButton.left - textSizeW - parentButton.width, parentButton.top,0,0)
        }
        else {
            layout.setMargins(parentButton.left + parentButtonWidthPx, parentButton.top,0,0)
        }

        //Load button look
        buttonFacialFeature.text = buttonText
        buttonFacialFeature.textSize = activityTextSize
        buttonFacialFeature.layoutParams = layout
        buttonFacialFeature.setLines(1)

        // add Button to layout
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            dynamicButtonsLayout.addView(buttonFacialFeature)
        }

        buttonFacialFeature.setOnClickListener()
        {
            facialReconstruction(face, analyse_image_view.drawable.toBitmap())
        }

        buttonFeatureIsActive = true
        return buttonFacialFeature
    }

    // Copies newFace into currentBitmap's face at position X and Y, returns the complete bitmap with copied face
    private fun computeFace(faceToReplaceX:Int, faceToReplaceY:Int, faceToReplaceHeight:Int, faceToReplaceWidth:Int, currentBitmap:Bitmap, newFace:Bitmap):Bitmap{

        // Need to resize newBitmap to faceToReplace dimensions !!
        val newFaceResized = createScaledBitmap(newFace , faceToReplaceWidth , faceToReplaceHeight, true)
        val modifiedBitmap = createBitmap(currentBitmap)

        val modifiedBitmapCanvas = Canvas(modifiedBitmap)
        val paint = Paint()
        paint.alpha = 255

        val mergedFace = createMergedFace(0.5f, newFaceResized, newFaceResized)

        modifiedBitmapCanvas.drawBitmap(newFaceResized, faceToReplaceX.toFloat(), faceToReplaceY.toFloat(), paint)

        return modifiedBitmap
    }

    // TODO : prendre un visage et non un carré noir
    private fun facialReconstruction(currentFace:Face, currentBitmap: Bitmap)
    {
        Toast.makeText(this, "Face reconstruction", Toast.LENGTH_SHORT).show()

        val faceGenerator = TensorModelManager()
        faceGenerator.loadModelDefault(this)
        if (faceGenerator.isOperational()){
            val newFace = createScaledBitmap(faceGenerator.generateFace(), currentFace.width.toInt(), currentFace.height.toInt(), true)
            var modifiedBitmap = computeFace(currentFace.position.x.toInt(), currentFace.position.y.toInt(), currentFace.height.toInt(), currentFace.width.toInt(), currentBitmap, newFace) // Need face to copy
            analyse_image_view.setImageDrawable(BitmapDrawable(resources, modifiedBitmap))
        }else{
            Log.d("ERROR", "Error when creating the faceGenerator")
        }
        faceGenerator.close()

    }

}
