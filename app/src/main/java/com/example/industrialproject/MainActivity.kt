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
import android.media.MediaScannerConnection
import android.graphics.Bitmap
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import android.view.WindowManager
import java.io.FileOutputStream
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity()
{
    private val PERMISSION_CODE = 1000
    private val IMAGE_CAPTURE_CODE = 1001
    var image_uri: Uri? = null
    var isGalleryChoosen= false
    var isCameraChoosen = false

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

    private fun saveImageToExternalStorage(finalBitmap: Bitmap) {
        val root = getExternalStoragePublicDirectory(DIRECTORY_PICTURES).toString()
        val myDir = File(root + "/" + BuildConfig.APPLICATION_ID + "_pictures")
        myDir.mkdirs()
        val tsLong = System.currentTimeMillis() / 1000
        val timeStamp = tsLong.toString()
        val fname = "Pic_$timeStamp.jpg"
        val file = File(myDir, fname)
        try {
            val out = FileOutputStream(file)
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Tell the media scanner about the new file so that it is
        // immediately available to the user.
        MediaScannerConnection.scanFile(
            this, arrayOf(file.toString()), null
        ) { path, uri ->
            Log.i("ExternalStorage", "Scanned $path:")
            Log.i("ExternalStorage", "-> uri=$uri")
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

        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Log.d("ERROR", "Error, can't create the image file")
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
                    isCameraChoosen = true
                    startActivityForResult(takePictureIntent, IMAGE_CAPTURE_CODE)
                }
            }
        }

    }

    private fun pickImageFromGallery()
    {
        //Intent to pick image
        val imageIntent = Intent(Intent.ACTION_GET_CONTENT)
        imageIntent.type = "image/*"
        isGalleryChoosen = true
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
        //called when image was captured from camera intent
        if (resultCode == Activity.RESULT_OK && data != null){

            var true_imag_uri = ""
            if(isGalleryChoosen){
                true_imag_uri = (data.data.toString())
                isGalleryChoosen = false
                Log.d("DEBUG", "Gallery action finished and processed")
            }else if(isCameraChoosen){
                true_imag_uri = image_uri.toString()
                isCameraChoosen = false
                val bitmap_temp = MediaStore.Images.Media.getBitmap(this.contentResolver, image_uri);
                saveImageToExternalStorage(bitmap_temp)
                Log.d("DEBUG", "Camera action finished and processed")
            }else {
                Log.d("DEBUG", "Error, action finished but no action code activated")
            }
            val intent = Intent(this, Analyse_Activity::class.java).apply {}
            Log.d("DEBUG", "Image URI is : $true_imag_uri")
            intent.putExtra("imageUri", true_imag_uri)
            startActivity(intent)
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.d("DEBUG", "Action cancelled")
        }

    }
    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            finishAffinity()
        }
        exitProcess(0)
    }

}
