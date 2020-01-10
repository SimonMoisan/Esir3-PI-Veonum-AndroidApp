package com.example.industrialproject

import android.R
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.graphics.createBitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.lang.Exception
import java.util.*

class TensorModelManager {

    private var interpreter : Interpreter? = null
    private var gpuDelegate : GpuDelegate? = null

    private val modelResultSizeWidth = 28
    private val modelResultSizeheight = 28

    // Function that load a tensorFlow lite model(.tflite) with a path to this model and create a interpreter wth it
    fun loadModelFromPath(path : String){
        val modelFile = File(path)

        if(tryGPU()){
            val gpuOption = Interpreter.Options().addDelegate(gpuDelegate)
            interpreter = Interpreter(modelFile, gpuOption)
        }else{
            interpreter = Interpreter(modelFile)
        }
    }

    // Function that load the default tensorFlow lite model(.tflite) create a interpreter wth it
    fun loadModelDefault(){
        val modelFile = File(Uri.parse("file:///android_asset/models/default_model.tflite").toString())

        if(tryGPU()){
            val gpuOption = Interpreter.Options().addDelegate(gpuDelegate)
            interpreter = Interpreter(modelFile, gpuOption)
        }else{
            interpreter = Interpreter(modelFile)
        }
    }

    // This function try to create a GPU manager for the interpreter. Return true if it was succeful and false if he fail.
    private fun tryGPU() : Boolean {
        var isGpuUsable = false
        try {
            gpuDelegate = GpuDelegate()
            isGpuUsable = true
            Log.d("DEBUG", "Tried to use GPU")
        }catch(e : Exception){
            Log.d("DEBUG", "Error when trying to use GPU, switching to CPU mode")
        }
        return isGpuUsable
    }

    //test if the interpreter is operational and can be used
    fun isOperational() : Boolean{
        return interpreter == null
    }

    // Main function that generate a random face
    fun generateFace() : Bitmap{

        val randomNoise = FloatArray(100)
        val rand = Random()

        for (x in 0..99){
            randomNoise[x] = rand.nextGaussian().toFloat()
        }

        val input : TensorBuffer = TensorBuffer.createDynamic(DataType.FLOAT32)
        input.loadArray(randomNoise)
        val output = TensorBuffer.createDynamic(DataType.UINT8)
        interpreter?.run(input.buffer, output.buffer)
        val generatedFace = createBitmap(modelResultSizeWidth, modelResultSizeheight)

        convertTensorBufferToBitmap(output, generatedFace)

        return generatedFace
    }

    // This function came from the official github of tensorFlow lite, in the experimental branch
    // It transform the buffer given by the model in the interpreter into a Bitmap, usable by classic android graphic functions
    private fun convertTensorBufferToBitmap(buffer: TensorBuffer, bitmap: Bitmap) {
        if (buffer.dataType != DataType.UINT8) {
            // We will add support to FLOAT format conversion in the future, as it may need other configs.
            throw UnsupportedOperationException(
                String.format(
                    "Converting TensorBuffer of type %s to Bitmap is not supported yet.",
                    buffer.dataType
                )
            )
        }
        val shape = buffer.shape
        require(!(shape.size != 3 || shape[0] <= 0 || shape[1] <= 0 || shape[2] != 3)) {
            String.format(
                "Buffer shape %s is not valid. 3D TensorBuffer with shape [w, h, 3] is required",
                Arrays.toString(shape)
            )
        }
        val h = shape[0]
        val w = shape[1]
        require(!(bitmap.width != w || bitmap.height != h)) {
            String.format(
                "Given bitmap has different width or height %s with the expected ones %s.",
                intArrayOf(bitmap.width, bitmap.height).contentToString(),
                intArrayOf(w, h).contentToString()
            )
        }
        require(bitmap.isMutable) { "Given bitmap is not mutable" }
        val intValues = IntArray(w * h)
        val rgbValues = buffer.intArray
        var i = 0
        var j = 0
        while (i < intValues.size) {
            val r = rgbValues[j++]
            val g = rgbValues[j++]
            val b = rgbValues[j++]
            intValues[i] = ((r shl 16) or (g shl 8) or b)
            i++
        }
        bitmap.setPixels(intValues, 0, w, 0, 0, w, h)
    }

}