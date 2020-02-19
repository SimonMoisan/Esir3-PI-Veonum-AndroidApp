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

// Transforms a triangle (list of Point) into an triangle (array of Point)
fun listToPointArray(srcList:MutableList<opencv_core.Point>):opencv_core.Point{
    val res = opencv_core.Point(3)
    for(i in 0 until 3){
        res.position(i.toLong()).x(srcList[i].x().toInt())
        res.position(i.toLong()).y(srcList[i].y().toInt())
    }
    return res
}

// Gets the index of the feature from the coordinates of the point
fun getIndex(listOfFeaturePoints:MutableList<opencv_core.Point2f>, pointToFind:opencv_core.Point2f):Int{
    for(i in 0 until listOfFeaturePoints.size){
        val currentPoint = listOfFeaturePoints[i]
        if(currentPoint.x()==pointToFind.x() && currentPoint.y()==pointToFind.y()){
            return i
        }
    }
    return -1
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

// Computes triangulation of second face features, from Delaunay Triangulation of first face features
// subDiv : subdiv2D containing points from first image
// featuresFirst : list of features of the first face
// featuresSecond : list of features of the second face
// Returns a list of triangles corresponding to the triangulation of the second face (in coordinates, not indexes)
fun triangulationFromSubdiv(subDiv: opencv_imgproc.Subdiv2D, featuresFirst:MutableList<opencv_core.Point2f>, featuresSecond:MutableList<opencv_core.Point2f>):MutableList<MutableList<opencv_core.Point2f>>{

    // Find the index in feature list for each point of each triangles in first face
    val trianglesFirstFace = subDivToTriangles(subDiv)
    val trianglesIndexes : MutableList<MutableList<Int>> = mutableListOf()
    for(triangle in trianglesFirstFace){

        val point1 = triangle[0]
        val point2 = triangle[1]
        val point3 = triangle[2]

        val index1 = getIndex(featuresFirst, point1)
        val index2 = getIndex(featuresFirst, point2)
        val index3 = getIndex(featuresFirst, point3)

        // Getting the indexes in the feature list instead of the coordinates that we get from subDiv2D
        if (index1 != -1 && index2 != -1 && index3 != -1){
            val triangleIndex : MutableList<Int> = mutableListOf()
            triangleIndex.add(index1)
            triangleIndex.add(index2)
            triangleIndex.add(index3)
            trianglesIndexes.add(triangleIndex)
        }
    }
    //Triangulating the second face from the first face delaunay triangulation
    val trianglesSecondFace : MutableList<MutableList<opencv_core.Point2f>> = mutableListOf()
    for (triangleIndex in trianglesIndexes){
        val point1 = featuresSecond[triangleIndex[0]]
        val point2 = featuresSecond[triangleIndex[1]]
        val point3 = featuresSecond[triangleIndex[2]]

        val triangleCurrent : MutableList<opencv_core.Point2f> = mutableListOf()
        triangleCurrent.add(point1)
        triangleCurrent.add(point2)
        triangleCurrent.add(point3)

        trianglesSecondFace.add(triangleCurrent)
    }
    return trianglesSecondFace
}

// Computes the affine transform between two triangles and applies it to srcMat
fun applyAffine(srcMat:opencv_core.Mat, rectMorphed:Rect, triangle1:MutableList<opencv_core.Point2f>, triangle2:MutableList<opencv_core.Point2f>):opencv_core.Mat{

    // Points of triangle 1
    val triangle1Points = listToPoint2fArray(triangle1)

    // Points of triangle 2
    val triangle2Points = listToPoint2fArray(triangle2)

    val resMat:Mat = Mat(rectMorphed.size(), srcMat.type())

    val warp = getAffineTransform(triangle1Points.position(0), triangle2Points.position(0))
    warpAffine(srcMat, resMat, warp, resMat.size())

    return resMat
}

// Creates a morphing and alpha blend of two triangles from img1 and img2 to imgDst
fun morphTriangles(img1: Mat, img2: Mat, imgDst: Mat, triangle1:MutableList<opencv_core.Point2f>, triangle2:MutableList<opencv_core.Point2f>, triangleMorphed:MutableList<opencv_core.Point2f>, alpha: Double):Mat{

    val triangle1Points = listToPoint2fArray(triangle1)
    val triangle2Points = listToPoint2fArray(triangle2)
    val triangleMorphedPoints = listToPoint2fArray(triangleMorphed)

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
    fillConvexPoly(mask, matRectInt, Scalar(1.0, 1.0, 1.0, 1.0), 8, 0)

    // Cropping img- with rect-
    val img1Rect = Mat(img1.clone(), rect1)
    val img2Rect = Mat(img2.clone(), rect2)

    val matDestCropped = Mat(imgDst.clone(), rectMorphed)

    val warpImage1 = applyAffine(img1Rect, rectMorphed, triangle1, triangleMorphed)
    val warpImage2 = applyAffine(img2Rect, rectMorphed, triangle2, triangleMorphed)

    val imgRect =  opencv_core.add(multiply(warpImage1,(1.0 - alpha)).asMat(), multiply(warpImage2, alpha).asMat()).asMat()
    val maskedImg = imgRect.mul(mask)

    val final = add(matDestCropped.mul(subtract(Scalar(1.0,1.0,1.0,1.0),mask).asMat()), maskedImg).asMat()

    val finalCroppedBitmap = myMatToBitmap(final)
    val dstBitmap = myMatToBitmap(imgDst)

    val dstCanvas = Canvas(dstBitmap)
    dstCanvas.drawBitmap(finalCroppedBitmap, rectMorphed.x().toFloat(), rectMorphed.y().toFloat(), Paint())

    return myBitmapToMat(dstBitmap)
}

// Creates a morphing and alpha blend of two triangles from img1 and img2 to imgDst
fun swapTriangles(img1: Mat, img2: Mat, imgDst: Mat, triangle1:MutableList<opencv_core.Point2f>, triangle2:MutableList<opencv_core.Point2f>, alpha: Double):Mat{

    val triangle1Points = listToPoint2fArray(triangle1)
    val triangle2Points = listToPoint2fArray(triangle2)

    val matRect1 = Mat(triangle1Points.position(0))
    val matRect2 = Mat(triangle2Points.position(0))

    val rect1 = boundingRect(matRect1)
    val rect2 = boundingRect(matRect2)

    val listRect1 : MutableList<opencv_core.Point2f> = mutableListOf()
    val listRect2 : MutableList<opencv_core.Point2f> = mutableListOf()
    val listRectMorph : MutableList<opencv_core.Point2f> = mutableListOf()
    val listRectInt : MutableList<opencv_core.Point> = mutableListOf()

    for(i in 0 until 3){
        listRectInt.add(Point( (triangle1[i].x() - rect1.x()).toInt(), (triangle1[i].y() - rect1.y()).toInt()) )

        listRect1.add(Point2f(triangle1[i].x() - rect1.x(), triangle1[i].y() - rect1.y()))
        listRect2.add(Point2f(triangle2[i].x() - rect2.x(), triangle2[i].y() - rect2.y()))
    }

    // Filling convex poly with mask
    val mask:Mat = opencv_core.Mat.zeros(rect1.height(), rect1.width(), CV_8UC4).asMat()
    val pointsRectInt = listToPointArray(listRectInt)
    val matRectInt = Mat(pointsRectInt.position(0))
    matRectInt.convertTo(matRectInt, CV_32S)
    fillConvexPoly(mask, matRectInt, Scalar(0.0, 0.0, 0.0, 255.0), 8, 0)

    // Cropping img- with rect-
    val img1Rect = Mat(img1.clone(), rect1)
    val img2Rect = Mat(img2.clone(), rect2)

    val matDestCropped = Mat(imgDst.clone(), rect1)

    val warpImage1 = applyAffine(img1Rect, rect1, triangle1, triangle2)

    val imgRect =  warpImage1
    val maskedImg = imgRect.mul(mask)

    val final = add(matDestCropped.mul(subtract(Scalar(1.0,1.0,1.0,1.0),mask).asMat()), maskedImg).asMat()

    val finalCroppedBitmap = myMatToBitmap(final)
    val dstBitmap = myMatToBitmap(imgDst)

    val dstCanvas = Canvas(dstBitmap)
    dstCanvas.drawBitmap(finalCroppedBitmap, rect1.x().toFloat(), rect1.y().toFloat(), Paint())

    return myBitmapToMat(dstBitmap)
}

// Merges two images together. The merging is controlled by alpha
fun createMergedFace(alpha:Float, currentFaceBitmap:Bitmap, currentFace: Face, newFaceBitmap:Bitmap, newFace:Face):Bitmap{

    // Sorting detected landmarks (because the detector doesn't always return the same numbers of features)
    val sortedCurrentLandmarks:MutableList<opencv_core.Point2f> = mutableListOf()
    val sortedNewLandmarks:MutableList<opencv_core.Point2f> = mutableListOf()
    for (currentLandmark in currentFace.landmarks){
        for (newLandmark in newFace.landmarks){
            if(currentLandmark.type == newLandmark.type){
                val pointCurrent = opencv_core.Point2f(currentLandmark.position.x, currentLandmark.position.y)
                sortedCurrentLandmarks.add(pointCurrent)
                val pointNew = opencv_core.Point2f(newLandmark.position.x, newLandmark.position.y)
                sortedNewLandmarks.add(pointNew)
                break
            }
        }
    }
    // Manually adding a point on the top middle of the images
    sortedCurrentLandmarks.add(opencv_core.Point2f(currentFaceBitmap.width/2.0f, 1.0f))
    sortedNewLandmarks.add(opencv_core.Point2f(newFaceBitmap.width/2.0f, 1.0f))
    sortedCurrentLandmarks.add(opencv_core.Point2f(currentFaceBitmap.width/2.0f, currentFaceBitmap.height.toFloat()-1.0f))
    sortedNewLandmarks.add(opencv_core.Point2f(newFaceBitmap.width/2.0f,  newFaceBitmap.height.toFloat()-1.0f))

    // Creating a rectangle that bound all the points
    val rectCurrent = opencv_core.Rect(0, 0, currentFaceBitmap.width, currentFaceBitmap.height)
    // Create an instance of Subdiv2D to get Delaunay triangulation
    val subCurrent = opencv_imgproc.Subdiv2D(rectCurrent)
    // Insert all the points into sub
    for (point in sortedCurrentLandmarks){
        subCurrent.insert(point)
    }
    val trianglesCurrent =  subDivToTriangles(subCurrent)
    val trianglesNew = triangulationFromSubdiv(subCurrent, sortedCurrentLandmarks, sortedNewLandmarks)

    // Create points for the merged image
    val sortedMergedLandmarks:MutableList<opencv_core.Point2f> = mutableListOf()
    for (i in 0 until  sortedCurrentLandmarks.size){
        val x = (1.0f - alpha)*sortedCurrentLandmarks[i].x() + alpha*sortedNewLandmarks[i].x()
        val y = (1.0f - alpha)*sortedCurrentLandmarks[i].y() + alpha*sortedNewLandmarks[i].y()
        val point = opencv_core.Point2f(x, y)
        //val point = opencv_core.Point2f(sortedCurrentLandmarks[i].x(), sortedCurrentLandmarks[i].y())
        sortedMergedLandmarks.add(point)
    }
    val trianglesMerged = triangulationFromSubdiv(subCurrent, sortedCurrentLandmarks, sortedMergedLandmarks)

    val currentFaceBitmap8888 = currentFaceBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val currentFaceMat = myBitmapToMat(currentFaceBitmap8888)
    val newFaceMat = myBitmapToMat(newFaceBitmap)

    var imgDst = Mat(currentFaceMat.size(), currentFaceMat.type())

    for(i in 0 until min(min(trianglesNew.size, trianglesMerged.size), trianglesCurrent.size)){

        val triangle1 = trianglesCurrent[i]
        val triangle2 = trianglesNew[i]
        val triangleMorphed = trianglesMerged[i]

        imgDst = morphTriangles(currentFaceMat, newFaceMat, imgDst, triangle1, triangle2, triangleMorphed, alpha.toDouble())
        //imgDst = swapTriangles(currentFaceMat, newFaceMat, imgDst, triangle1, triangle2, alpha.toDouble())

        val point1 = opencv_core.Point(triangle1[0].x().toInt(), triangle1[0].y().toInt())
        val point2 = opencv_core.Point(triangle1[1].x().toInt(), triangle1[1].y().toInt())
        val point3 = opencv_core.Point(triangle1[2].x().toInt(), triangle1[2].y().toInt())
        //circle(imgDst, point1, 10,Scalar(0.0, 255.0, 0.0, 255.0),-1,8,0)
        //circle(imgDst, point2, 10,Scalar(0.0, 255.0, 0.0, 255.0),-1,8,0)
        //circle(imgDst, point3, 10,Scalar(0.0, 255.0, 0.0, 255.0),-1,8,0)

        val pointNew1 = opencv_core.Point(triangle2[0].x().toInt(), triangle2[0].y().toInt())
        val pointNew2 = opencv_core.Point(triangle2[1].x().toInt(), triangle2[1].y().toInt())
        val pointNew3 = opencv_core.Point(triangle2[2].x().toInt(), triangle2[2].y().toInt())
        //circle(imgDst, pointNew1, 10,Scalar(0.0, 0.0, 255.0, 255.0),-1,8,0)
        //circle(imgDst, pointNew2, 10,Scalar(0.0, 0.0, 255.0, 255.0),-1,8,0)
        //circle(imgDst, pointNew3, 10,Scalar(0.0, 0.0, 255.0, 255.0),-1,8,0)

        val pointM1 = opencv_core.Point(triangleMorphed[0].x().toInt(), triangle2[0].y().toInt())
        val pointM2 = opencv_core.Point(triangleMorphed[1].x().toInt(), triangle2[1].y().toInt())
        val pointM3 = opencv_core.Point(triangleMorphed[2].x().toInt(), triangle2[2].y().toInt())
        //circle(imgDst, pointM1, 10,Scalar(0.0, 0.0, 0.0, 255.0),-1,8,0)
        //circle(imgDst, pointM2, 10,Scalar(0.0, 0.0, 0.0, 255.0),-1,8,0)
        //circle(imgDst, pointM3, 10,Scalar(0.0, 0.0, 0.0, 255.0),-1,8,0)

        //line(imgDst, point1, point2, Scalar(255.0,0.0,0.0,255.0), 10, 8, 0)
        //line(imgDst, point2, point3, Scalar(255.0,0.0,0.0,255.0), 10, 8, 0)
        //line(imgDst, point1, point3, Scalar(255.0,0.0,0.0,255.0), 10, 8, 0)

    }


    return myMatToBitmap(imgDst)
}