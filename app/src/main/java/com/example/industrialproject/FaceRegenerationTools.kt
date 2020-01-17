package com.example.industrialproject

import android.graphics.Bitmap
import org.bytedeco.javacpp.opencv_core
import org.bytedeco.javacpp.opencv_core.CV_32F
import org.bytedeco.javacv.OpenCVFrameConverter
import org.bytedeco.javacv.Java2DFrameUtils
import org.bytedeco.javacv.AndroidFrameConverter
import org.bytedeco.javacv.Frame

fun test():Int{
    return 1
}

fun myBitmapToMat(srcBitmap: Bitmap):opencv_core.Mat{
    val frame = AndroidFrameConverter().convert(srcBitmap)
    return Java2DFrameUtils.toMat(frame)
}

fun myMatToBitmap(srcMat: opencv_core.Mat):Bitmap{
    val frame = Java2DFrameUtils.toFrame(srcMat)
    return AndroidFrameConverter().convert(frame)
}

// Merges two images together. The merging is controlled by alpha
fun createMergedFace(alpha:Float, originalBitmap:Bitmap, generatedBitmap:Bitmap):Bitmap{

    //val originalMat = opencv_core.Mat.zeros(originalBitmap.height, originalBitmap.width, CV_32F)
    val originalMat = myBitmapToMat(originalBitmap)

    //val generatedMat = opencv_core.Mat.zeros(originalBitmap.height, originalBitmap.width, CV_32F)
    val generatedMat = myBitmapToMat(generatedBitmap)

    val mergedMat:opencv_core.Mat = opencv_core.Mat.zeros(originalBitmap.height, originalBitmap.width, CV_32F).asMat()

    // Step 1 : detect features in both faces

    // Step 2 : match the features

    // Step 3 : Delaunay triangulation

    // Step 4 : compute affine transforms between triangles

    // Step 5 : morph images from alpha and affine transforms

    // Step 6 : convert back to RBG Bitmap

    //val res:Bitmap = Bitmap.createBitmap(generatedBitmap)
    val res = myMatToBitmap(mergedMat)
    return res
}