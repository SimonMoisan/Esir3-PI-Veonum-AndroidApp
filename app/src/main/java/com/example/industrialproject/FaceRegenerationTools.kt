package com.example.industrialproject

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import androidx.core.graphics.createBitmap
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.Landmark
import org.bytedeco.javacpp.FloatPointer
import org.bytedeco.javacpp.opencv_core
import org.bytedeco.javacpp.opencv_core.*
import org.bytedeco.javacpp.opencv_imgproc
import org.bytedeco.javacv.AndroidFrameConverter
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.OpenCVFrameConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.*
import kotlin.experimental.and
import org.bytedeco.javacpp.opencv_imgproc.*
import kotlin.math.min


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
    `in`.put(buffer as ByteBuffer)

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

// Transforms a triangle (list of Point2f) into an triangle (array of Point2f)
fun listToPoint2fArray(srcList:MutableList<opencv_core.Point2f>):opencv_core.Point2f{
    val res = opencv_core.Point2f(3)
    for(i in 0 until 3){
        res.position(i.toLong()).x(srcList[i].x())
        res.position(i.toLong()).y(srcList[i].y())
    }
    return res
}

// Computes the affine transform between two triangles and applies it to srcMat
fun applyAffine(srcMat:opencv_core.Mat, rectMorphed:Rect, triangle1:MutableList<opencv_core.Point2f>, triangle2:MutableList<opencv_core.Point2f>):opencv_core.Mat{

    // Points of triangle 1
    val triangle1Points = listToPoint2fArray(triangle1)

    // Points of triangle 2
    val triangle2Points = listToPoint2fArray(triangle2)

    val resMat:Mat = Mat(rectMorphed.size(), srcMat.type())

    val warp = getAffineTransform(triangle1Points.position(0), triangle2Points.position(0))
    warpAffine(srcMat, resMat, warp, resMat.size(), INTER_LINEAR, BORDER_REFLECT_101, Scalar(0))

    return resMat
}

// Creates a morphing and alpha blend of two triangles from img1 and img2 to imgDst
fun morphTriangles(img1: Mat, img2: Mat, imgDst: Mat, triangle1:MutableList<opencv_core.Point2f>, triangle2:MutableList<opencv_core.Point2f>, triangleMorphed:MutableList<opencv_core.Point2f>, alpha: Double):Mat{

    val triangle1Points = opencv_core.Point2f(3)
    for(i in 0 until 3){
        triangle1Points.position(i.toLong()).x(triangle1[i].x())
        triangle1Points.position(i.toLong()).y(triangle1[i].y())
    }
    val triangle2Points = opencv_core.Point2f(3)
    for(i in 0 until 3){
        triangle2Points.position(i.toLong()).x(triangle2[i].x())
        triangle2Points.position(i.toLong()).y(triangle2[i].y())
    }
    val triangleMorphedPoints = opencv_core.Point2f(3)
    for(i in 0 until 3){
        triangleMorphedPoints.position(i.toLong()).x(triangleMorphed[i].x())
        triangleMorphedPoints.position(i.toLong()).y(triangleMorphed[i].y())
    }

    val matRect1 = Mat(triangle1Points.position(0))
    val matRect2 = Mat(triangle2Points.position(0))
    val matRectMorphed = Mat(triangleMorphedPoints.position(0))

    val rect1 = boundingRect(matRect1)
    val rect2 = boundingRect(matRect2)
    val rectMorphed = boundingRect(matRectMorphed)

    val listRect1 : MutableList<opencv_core.Point2f> = mutableListOf()
    val listRect2 : MutableList<opencv_core.Point2f> = mutableListOf()
    val listRectMorph : MutableList<opencv_core.Point2f> = mutableListOf()
    val listRectInt : MutableList<opencv_core.Point> = mutableListOf()

    for(i in 0 until 3){
        listRectMorph.add(Point2f(triangleMorphed[i].x() - rectMorphed.x(), triangleMorphed[i].y() - rectMorphed.y()))
        listRectInt.add(Point( (triangleMorphed[i].x() - rectMorphed.x()).toInt(), (triangleMorphed[i].y() - rectMorphed.y()).toInt()) )

        listRect1.add(Point2f(triangle1[i].x() - rect1.x(), triangle1[i].y() - rect1.y()))
        listRect2.add(Point2f(triangle2[i].x() - rect2.x(), triangle2[i].y() - rect2.y()))
    }

    // Filling convex poly with mask
    val mask:Mat = opencv_core.Mat.zeros(rectMorphed.height(), rectMorphed.width(), CV_8UC4).asMat()
    // Need to convert from list of point to mat
    val pointsRectInt = opencv_core.Point2f(3)
    for(i in 0 until 3){
        pointsRectInt.position(i.toLong()).x(listRectInt[i].x().toFloat())
        pointsRectInt.position(i.toLong()).y(listRectInt[i].y().toFloat())
    }
    val matRectInt = Mat(pointsRectInt.position(0))
    matRectInt.convertTo(matRectInt, CV_32S)
    fillConvexPoly(mask, matRectInt, Scalar(1.0, 1.0, 1.0, 1.0), 16, 0)

    val img1Rect = Mat(img1.clone(), rect1)
    val img2Rect = Mat(img2.clone(), rect2)

    val warpImage1 = applyAffine(img1Rect, rectMorphed, triangle1, triangleMorphed)
    val warpImage2 = applyAffine(img2Rect, rectMorphed, triangle2, triangleMorphed)

    val imgInter1 = multiply(warpImage1,(1.0 - alpha)).asMat()
    val imgInter2 = multiply(warpImage2, alpha).asMat()
    val imgRect =  opencv_core.add(imgInter1, imgInter2).asMat()

    val maskedImg = imgRect.mul(mask)

    val matDestCropped = Mat(imgDst.clone(), rectMorphed)

    val matSub = subtract(Scalar(1.0,1.0,1.0,1.0),mask).asMat()

    val matDestCroppedMorphed = matDestCropped.mul(matSub)

    val final = add(matDestCroppedMorphed, maskedImg).asMat()

    val finalCroppedBitmap = myMatToBitmap(final)
    val dstBitmap = myMatToBitmap(imgDst)

    val dstCanvas = Canvas(dstBitmap)
    val paint = Paint()
    dstCanvas.drawBitmap(finalCroppedBitmap, rectMorphed.x().toFloat(), rectMorphed.y().toFloat(), paint)

    return myBitmapToMat(dstBitmap)
}

// Returns a list of triangles from a subDiv2D containing points
fun subDivToTriangles(subDiv: opencv_imgproc.Subdiv2D): MutableList<MutableList<opencv_core.Point2f>>{

    val triangles: MutableList<MutableList<opencv_core.Point2f>> = mutableListOf()
    val trianglesPoints = FloatPointer()
    subDiv.getTriangleList(trianglesPoints)

    // trianglesPoints.limit()*6 --> number of coordinates
    // trianglesPoints.limit() --> number of triangles
    // This is fixed in later updates of JavaCV (<=1.5) but we fail to install it

    for(i in 0 until ((trianglesPoints.limit()*6) - 5) step 6){
        val x1 = trianglesPoints.get(i)
        val y1 = trianglesPoints.get(i+1)
        val p1 = opencv_core.Point2f(x1, y1)

        val x2 = trianglesPoints.get(i+2)
        val y2 = trianglesPoints.get(i+3)
        val p2 = opencv_core.Point2f(x2, y2)

        val x3 = trianglesPoints.get(i+4)
        val y3 = trianglesPoints.get(i+5)
        val p3 = opencv_core.Point2f(x3, y3)

        val currentTriangle: MutableList<opencv_core.Point2f> = mutableListOf()
        currentTriangle.add(p1)
        currentTriangle.add(p2)
        currentTriangle.add(p3)

        triangles.add(currentTriangle)
    }
    return triangles
}

// Merges two images together. The merging is controlled by alpha
fun createMergedFace(alpha:Float, currentFaceBitmap:Bitmap, currentFace: Face, newFaceBitmap:Bitmap, newFace:Face):Bitmap{

    // Sorting detected landmarks (because the detector doesn't always return the same numbers of features)
    val sortedCurrentLandmarks:MutableList<Landmark> = mutableListOf()
    val sortedNewLandmarks:MutableList<Landmark> = mutableListOf()
    for (currentLandmark in currentFace.landmarks){
        for (newLandmark in newFace.landmarks){
            if(currentLandmark.type == newLandmark.type){
                sortedCurrentLandmarks.add(currentLandmark)
                sortedNewLandmarks.add(newLandmark)
                break
            }
        }
    }

    // Creating a rectangle that bound all the points
    val rectCurrent = opencv_core.Rect(0, 0, currentFaceBitmap.width, currentFaceBitmap.height)
    // Create an instance of Subdiv2D to get Delaunay triangulation
    val subCurrent = opencv_imgproc.Subdiv2D(rectCurrent)
    // Insert all the points into sub
    for (landmark in sortedCurrentLandmarks){
        val point = opencv_core.Point2f(landmark.position.x, landmark.position.y)
        subCurrent.insert(point)
    }
    val lastPointCurrent = opencv_core.Point2f(currentFaceBitmap.width/2.0f, 0.0f)
    subCurrent.insert(lastPointCurrent)

    // Process triangles again for newFace
    val rectNew = opencv_core.Rect(0, 0, newFaceBitmap.width, newFaceBitmap.height)
    val subNew = opencv_imgproc.Subdiv2D(rectNew)
    for (landmark in sortedNewLandmarks){
        val point = opencv_core.Point2f(landmark.position.x, landmark.position.y)
        subNew.insert(point)
    }
    val lastPointNew = opencv_core.Point2f(newFaceBitmap.width/2.0f, 0.0f)
    subNew.insert(lastPointNew)

    // Create points for the merged image
    val subMerged = opencv_imgproc.Subdiv2D(rectCurrent)
    for (i in 0 until  sortedCurrentLandmarks.size){
        val x = (1 - alpha)*sortedCurrentLandmarks[i].position.x + alpha*sortedNewLandmarks[i].position.x
        val y = (1 - alpha)*sortedCurrentLandmarks[i].position.y + alpha*sortedNewLandmarks[i].position.y
        val point = opencv_core.Point2f(x, y)
        subMerged.insert(point)
    }
    val xLast = (1 - alpha)* lastPointCurrent.x() + alpha * lastPointNew.x()
    val yLast = (1 - alpha)* lastPointCurrent.y() + alpha * lastPointNew.y()
    val point = opencv_core.Point2f(xLast, yLast)
    subMerged.insert(point)

    // Getting the triangles from the points in subDiv2D
    val trianglesCurrent = subDivToTriangles(subCurrent)
    val trianglesNew = subDivToTriangles(subNew)
    val trianglesMerged = subDivToTriangles(subMerged)

    val currentFaceBitmap8888 = currentFaceBitmap.copy(Bitmap.Config.RGB_565, true)

    val currentFaceMat = myBitmapToMat(currentFaceBitmap8888)
    val newFaceMat = myBitmapToMat(newFaceBitmap)

    var imgDst = Mat(currentFaceBitmap.height, currentFaceBitmap.width, newFaceMat.type())

    Log.d("DEBUG", "Size of sortedCurrentLandmarks: " + sortedCurrentLandmarks.size)
    Log.d("DEBUG", "Size of sortedNewLandmarks : " + sortedNewLandmarks.size)

    Log.d("DEBUG", "Size of trianglesCurrent: " + trianglesCurrent.size)
    Log.d("DEBUG", "Size of trianglesNew : " + trianglesNew.size)
    Log.d("DEBUG", "Size of trianglesMerged : " + trianglesMerged.size)

    for(i in 0 until min(min(trianglesNew.size, trianglesMerged.size), trianglesCurrent.size)){

        val triangle1 = trianglesCurrent[i]
        val triangle2 = trianglesNew[i]
        val triangleMorphed = trianglesMerged[i]

        imgDst = morphTriangles(currentFaceMat, newFaceMat, imgDst, triangle1, triangle2, triangleMorphed, alpha.toDouble())

    }
    return myMatToBitmap(imgDst)
}