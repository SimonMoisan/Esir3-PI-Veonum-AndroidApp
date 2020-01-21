package com.example.industrialproject

import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.vision.face.Face
import org.bytedeco.javacpp.opencv_core
import org.bytedeco.javacpp.opencv_core.CV_32F
import org.bytedeco.javacv.AndroidFrameConverter
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.OpenCVFrameConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.experimental.and


internal fun gray2rgba(
    `in`: ByteBuffer,
    width: Int,
    height: Int,
    stride: Int,
    rowBytes: Int
): ByteBuffer {
    var buffer : ByteBuffer? = null
    var row: ByteArray? = null

    if (buffer == null || buffer.capacity() < height * rowBytes) {
        buffer = ByteBuffer.allocate(height * rowBytes)
    }
    if (row == null || row.size != stride)
        row = ByteArray(stride)
    for (y in 0 until height) {
        `in`.position(y * stride)
        `in`.get(row)
        for (x in 0 until width) {
            // GRAY -> RGBA
            val b = row[x]
            val rgba = (b and 0xFF.toByte()).toInt() shl 24 or (
                    (b and 0xFF.toByte()).toInt() shl 16) or (
                    (b and 0xFF.toByte()).toInt() shl 8) or 0xff
            buffer!!.putInt(y * rowBytes + 4 * x, rgba)
        }
    }
    return buffer!!
}

internal fun bgr2rgba(
    `in`: ByteBuffer,
    width: Int,
    height: Int,
    stride: Int,
    rowBytes: Int
): ByteBuffer {
    var buffer : ByteBuffer? = null
    var `in` = `in`
    if (`in`.order() != ByteOrder.LITTLE_ENDIAN) {
        `in` = `in`.order(ByteOrder.LITTLE_ENDIAN)
    }
    if (buffer == null || buffer.capacity() < height * rowBytes) {
        buffer = ByteBuffer.allocate(height * rowBytes)
    }
    for (y in 0 until height) {
        for (x in 0 until width) {
            // BGR -> RGBA
            val rgb: Int
            if (x < width - 1 || y < height - 1) {
                rgb = `in`.getInt(y * stride + 3 * x)
            } else {
                val b = `in`.get(y * stride + 3 * x) and 0xFF.toByte()
                val g = `in`.get(y * stride + 3 * x + 1) and 0xFF.toByte()
                val r = `in`.get(y * stride + 3 * x + 2) and 0xFF.toByte()
                rgb = r.toInt() shl 16 or (g.toInt() shl 8) or b.toInt()
            }
            buffer!!.putInt(y * rowBytes + 4 * x, rgb shl 8 or 0xff)
        }
    }
    return buffer!!
}
// Converts JavaCV Frame to Android Bitmap
fun myFrameToBitmap(frame : Frame):Bitmap?{

    var bitmap: Bitmap? = null

    if (frame?.image == null) {
        return null
    }

    var config: Bitmap.Config? = null
    when (frame.imageChannels) {
        2 -> config = Bitmap.Config.RGB_565
        1, 3, 4 -> config = Bitmap.Config.ARGB_8888
        else -> assert(false)
    }

    if (bitmap == null || bitmap.width != frame.imageWidth
        || bitmap.height != frame.imageHeight || bitmap.config != config
    ) {
        bitmap = Bitmap.createBitmap(frame.imageWidth, frame.imageHeight, config!!)
    }
    val buffer = frame.image[0]

    val `in` = ByteBuffer.allocate(buffer.capacity() * 4)
    `in`.asFloatBuffer().put(buffer as FloatBuffer)

    val width = frame.imageWidth
    val height = frame.imageHeight
    val stride = frame.imageStride
    val rowBytes = bitmap!!.rowBytes
    if (frame.imageChannels == 1) {
        val bufferGray2rgba = gray2rgba(`in`, width, height, stride, rowBytes)
        bitmap.copyPixelsFromBuffer(bufferGray2rgba.position(0))
    } else if (frame.imageChannels == 3) {
        val bufferBgr2rgba = bgr2rgba(`in`, width, height, stride, rowBytes)
        bitmap.copyPixelsFromBuffer(bufferBgr2rgba.position(0))
    } else {
        // assume matching strides
        bitmap.copyPixelsFromBuffer(`in`.position(0))
    }
    return bitmap
}

// Converts Android Bitmap to JavaCV Mat
fun myBitmapToMat(srcBitmap: Bitmap):opencv_core.Mat{
    val frame = AndroidFrameConverter().convert(srcBitmap)
    return OpenCVFrameConverter.ToMat().convert(frame)
}

// Converts JavaCV Mat to Android Bitmap
fun myMatToBitmap(srcMat: opencv_core.Mat):Bitmap{
    val frame:Frame = OpenCVFrameConverter.ToMat().convert(srcMat)
    return myFrameToBitmap(frame)!!
}

// Merges two images together. The merging is controlled by alpha
fun createMergedFace(alpha:Float, currentFaceBitmap:Bitmap, currentFace: Face, newFaceBitmap:Bitmap, newFace:Face):Bitmap{

    //val originalMat = opencv_core.Mat.zeros(originalBitmap.height, originalBitmap.width, CV_32F)
    //val originalMat = myBitmapToMat(originalBitmap)

    //val generatedMat = opencv_core.Mat.zeros(originalBitmap.height, originalBitmap.width, CV_32F)
    //val generatedMat = myBitmapToMat(generatedBitmap)

    val mergedMat:opencv_core.Mat = opencv_core.Mat.zeros(currentFaceBitmap.height, currentFaceBitmap.width, CV_32F).asMat()

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