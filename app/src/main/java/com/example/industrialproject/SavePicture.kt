package com.example.industrialproject

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream

fun saveImageToExternalStorage(finalBitmap: Bitmap, context : Context) {



    val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
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
        context, arrayOf(file.toString()), null
    ) { path, uri ->
        Log.i("ExternalStorage", "Scanned $path:")
        Log.i("ExternalStorage", "-> uri=$uri")
    }
}

fun rotate(bitmap: Bitmap, degree: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height

    val mtx = Matrix()
    mtx.postRotate(degree.toFloat())

    return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true)
}