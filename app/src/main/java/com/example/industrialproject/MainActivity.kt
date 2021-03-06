package com.example.industrialproject

import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import android.media.ExifInterface
import android.view.WindowManager
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity()
{
    private val PERMISSION_CODE = 1000
    private val IMAGE_CAPTURE_CODE = 1001
    var image_uri: Uri? = null
    var isGallery = false
    var isCamera = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_main)

        //Button get camera image
        capture_btn.setOnClickListener {
            //if system os is Marshmallow or Above, we need to request runtime permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                {
                    //permission was not enabled
                    val permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    //show popup to request permission
                    requestPermissions(permission, PERMISSION_CODE)
                }
                else
                {
                    //permission already granted
                    openCamera()
                }
            }
            else
            {
                //system os is < marshmallow
                openCamera()
            }
        }

        //Button get image from gallery
        imageGallery_btn.setOnClickListener {
            //if system os is Marshmallow or Above, we need to request runtime permission
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
                    pickImageFromGallery()
                }
            }
            else
            {
                //system os is < marshmallow
                pickImageFromGallery()
            }
        }

    }



    @Throws(IOException::class)
    private fun createImageFile(): File {

        // Create an image file name
        val tsLong = System.currentTimeMillis() / 1000
        val timeStamp = tsLong.toString()
        val storageDir: File = filesDir
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
        }

    }


    private fun openCamera()
    {
        isCamera = true

        try{
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                // Ensure that there's a camera activity to handle the intent
                takePictureIntent.resolveActivity(packageManager)?.also {
                    // Create the File where the photo should go
                    val photoFile: File? = try {
                        createImageFile()
                    } catch (ex: IOException) {
                        // Error occurred while creating the File
                        Log.d("ERROR", "Error, can't create the image file")
                        ex.printStackTrace()
                        null
                    }
                    // Continue only if the File was successfully created
                    photoFile?.also {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            this,
                            BuildConfig.APPLICATION_ID + ".provider",
                            it
                        )

                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        image_uri = photoURI

                        startActivityForResult(takePictureIntent, IMAGE_CAPTURE_CODE)
                    }
                }
            }
        }catch(e: Exception){
            Log.d("ERROR" , "Error in openCamera")
            e.printStackTrace()
            isCamera = false
            throw e
        }

    }

    private fun pickImageFromGallery()
    {
        //Intent to pick image
        isGallery = true
        val imageIntent = Intent(Intent.ACTION_GET_CONTENT)
        imageIntent.type = "image/*"
        image_uri = null
        startActivityForResult(imageIntent, IMAGE_PICK_CODE)
    }

    companion object
    {
        //image pick code
        private val IMAGE_PICK_CODE = 1000;
        //Permission code
        private val PERMISSION_CODE = 1001;
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        //called when user presses ALLOW or DENY from Permission Request Popup
        when(requestCode){
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.d("Debug", "Permissions accepted")
                    //permission from popup was granted
                    //pickImageFromGallery()
                    //openCamera()
                }
                else
                {
                    //permission from popup was denied
                    Log.d("Debug", "Permissions denied")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("DEBUG", "ActivityResult detected")
        //called when image was captured from camera intent
        if (resultCode == Activity.RESULT_OK){
            var trueImagUri = ""
            if(isGallery && data != null){
                trueImagUri = (data.data.toString())
                if (trueImagUri == "" || trueImagUri == null){
                    Log.d("ERROR", "Gallery action didn't return any image")
                }
                isGallery = false
                Log.d("DEBUG", "Gallery action finished and processed")
                Log.d("DEBUG", "TrueImageUri = $trueImagUri")
            }else if(isCamera){
                trueImagUri = image_uri.toString()
                if (trueImagUri == "" || trueImagUri == null){
                    Log.d("ERROR", "Camera action didn't return any image")
                }
                isCamera = false
                var bitmapTemp = MediaStore.Images.Media.getBitmap(this.contentResolver, image_uri)

                //Rotate Picture if exif code of the picture say it need to be rotated
                val exif = ExifInterface(contentResolver.openInputStream(image_uri))
                when {
                    exif.getAttribute(ExifInterface.TAG_ORIENTATION) == "6" -> bitmapTemp=rotate(bitmapTemp, 90)
                    exif.getAttribute(ExifInterface.TAG_ORIENTATION) == "8" -> bitmapTemp=rotate(bitmapTemp, 270)
                    exif.getAttribute(ExifInterface.TAG_ORIENTATION) == "3" -> bitmapTemp=rotate(bitmapTemp, 180)
                }

                saveImageToExternalStorage(bitmapTemp, this)
                Log.d("DEBUG", "Camera action finished and processed")
                Log.d("DEBUG", "TrueImageUri = $trueImagUri")
            }else{
                Log.d("DEBUG", "Error, action finished but no action code activated")
            }

            val intent = Intent(this, AnalyseActivity::class.java).apply {}
            Log.d("DEBUG", "Image URI is : $trueImagUri")
            intent.putExtra("imageUri", trueImagUri)
            startActivity(intent)
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.d("DEBUG", "Action cancelled")
        } else {
            Log.d("ERROR", "ResultCode not recognized. ResultCode is $resultCode")
        }

    }
    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            finishAffinity()
        }
        exitProcess(0)
    }

}
