package com.example.industrialproject

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.lang.Exception
import java.util.*
import java.nio.ByteBuffer
import java.io.*
import java.nio.ByteOrder

// Manager Class that stock the TensorFlow model and the function to use it
// Can be created multiple time for different models (if we have different models
class TensorModelManager {

    // Objects that stock the tensor flow lite objects like the model and the GPU link usable by TensorFlow
    private var interpreter : Interpreter? = null
    private var gpuDelegate : GpuDelegate? = null

    // Size of the model output image. We need it here because we can't know it by analysing the model its output data
    private var modelResultSizeWidth = 64
    private var modelResultSizeHeight = 64

    // Size of the random noise model input
    private var noiseInputSize = 256

    // Function that load a tensorFlow lite model(.tflite) with a path to this model and create a interpreter wth it
    fun loadModelFromPath(context : Context, fileName : String){

        val modelFile = inputStreamToAByteBuffer(context.assets.open(fileName))

        if(tryGPU()){
            val gpuOption = Interpreter.Options().addDelegate(gpuDelegate)
            interpreter = Interpreter(modelFile!!, gpuOption)
        }else{
            interpreter = Interpreter(modelFile!!)
        }
    }

    // Function that load the default (default_model.tflite in the asset) tensorFlow lite model(.tflite) and create a tensor flow lite interpreter object
    fun loadModelDefault(context : Context){

        val modelFile = inputStreamToAByteBuffer(context.assets.open("default_model.tflite"))

        if(tryGPU()){
            val gpuOption = Interpreter.Options().addDelegate(gpuDelegate)
            interpreter = Interpreter(modelFile!!, gpuOption)
        }else{
            interpreter = Interpreter(modelFile!!)
        }
    }

    // Setter of the width of the returned image. Have to be adjusted manually in function of the model ouput
    fun bitmapWidth(newWidth : Int) {
        if (newWidth > 0){
            modelResultSizeWidth = newWidth
        }
    }

    // Setter of the height of the returned image. Have to be adjusted manually in function of the model ouput
    fun bitmapHeight(newHeight : Int) {
        if (newHeight > 0){
            modelResultSizeHeight = newHeight
        }
    }

    // This function try to create a GPU manager for the interpreter. Return true if it was successful and false if he fail.
    // IMPROVEMENT : We can't know if the GPU is usable or not if we don't try it. But if we try it and it fails, it makes a system error which crash the app
    // Will not be a problem for all newer mobile devices, only for older devices with nonexistent or strange GPU implementation.
    private fun tryGPU() : Boolean {

        return false
        var isGpuUsable = false

        // This try and and catch can't really know if the GPU can be used or not. See the IMPROVEMENT section of this function
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
        return interpreter != null
    }

    // Main function that generate a random face
    fun generateFace() : Bitmap{

        val randomNoise = FloatArray(noiseInputSize)
        val rand = Random()

        for (x in 0 until noiseInputSize){
            randomNoise[x] = rand.nextGaussian().toFloat()
        }

        val input : TensorBuffer = TensorBuffer.createFixedSize(intArrayOf(1,noiseInputSize), DataType.FLOAT32)
        input.loadArray(randomNoise)
        val output = TensorBuffer.createFixedSize(intArrayOf(1,modelResultSizeWidth,modelResultSizeHeight,3),DataType.FLOAT32)
        interpreter?.run(input.buffer, output.buffer)

        val resRaw = output.buffer

        return getOutputImage(resRaw)
    }

    // Function that transform the output of the model (binary buffer) into a usable bitmap
    // WARNING : Depend very heavily on the model used for image generation !
    // IMPROVEMENT : Make multiple function for multiple types of models
    private fun getOutputImage(output: ByteBuffer): Bitmap {

        // Needed to make sure we work at the beginning of the buffer
        output.rewind()

        // Create a bitmap of the specified size, which will be filled pixel by pixel with the output data of the model.
        val bitmap = Bitmap.createBitmap(modelResultSizeWidth, modelResultSizeHeight, Bitmap.Config.ARGB_8888)
        // This line and the for after are the reason why we need to know the EXACT size of the represented picture the model generated
        val pixels = IntArray(modelResultSizeWidth * modelResultSizeHeight)
        for (i in 0 until modelResultSizeWidth * modelResultSizeHeight) {
            val a = 0xFF // No need for alpha

            // In the result of our models, we have a float array with float between -1 and 1
            // and we want to get value between 0 and 255 to have a valid RGB representation
            // Model specific, need to be changed if the model output data contain float between 0 and 1 for example
            val r = ((output.float + 1) / 2) * 255.0f
            val g = ((output.float + 1) / 2) * 255.0f
            val b = ((output.float + 1) / 2) * 255.0f

            // Rebuilding the pixel values with alpha, red, green and blue values in this order (ARGB)
            // Depend on the model and on the how the image will be show after
            pixels[i] = a shl 24 or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
        }
        bitmap.setPixels(pixels, 0, modelResultSizeWidth, 0, 0, modelResultSizeWidth, modelResultSizeHeight)
        return bitmap
    }

    // Utility function that take a Java InputStream and make a Java ByteBuffer with it
    // Function used to load the model into usable data
    // One of the only way to use Android assets files
    // IMPROVEMENT : Work well and is stable but it is a workaround to load file in the assets folder. The android asset folder is not made to be used like this.
    // IMPROVEMENT : For the future, we need to make a true way to save and load models, maybe with a manager that download model(s) in the Android file system AFTER the installation of this app
    private fun inputStreamToAByteBuffer(inputS : InputStream): ByteBuffer? {

        val buffer = ByteArrayOutputStream()
        var nRead: Int
        val data = ByteArray(1024) // Parameter that can be modified in function of the device hardware performance to accelerate model load speed. Hasn't a lot of impact. More wise to let it at 1024

        // Reading parameter iterator (Java InputStream) and putting the data read into the buffer (Java ByteBuffer)
        nRead = inputS.read(data, 0, data.size)
        while (nRead != -1) {
            buffer.write(data, 0, nRead)
            nRead = inputS.read(data, 0, data.size)
        }

        buffer.flush()
        val bytes = buffer.toByteArray()
        val byteBuffer = ByteBuffer.allocateDirect(bytes.size)
        byteBuffer.order(ByteOrder.nativeOrder()) // Reorganize the buffer with the data true order. Without it we will have the data but not necessary in the right order
        byteBuffer.put(bytes)

        return byteBuffer
    }

    // function to set the ramdom noise model input size
    // 256 by default
    fun setInputSize(inputSize : Int){
        noiseInputSize = inputSize
    }

    // function that close properly the interpreter
    fun close() {
        interpreter!!.close()
    }
}