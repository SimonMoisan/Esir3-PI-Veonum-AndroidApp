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
import android.R.attr.x



class TensorModelManager {

    private var interpreter : Interpreter? = null
    private var gpuDelegate : GpuDelegate? = null
    private var modelResultSizeWidth = 64
    private var modelResultSizeHeight = 64

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

    // Function that load the default tensorFlow lite model(.tflite) create a interpreter wth it
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

    // This function try to create a GPU manager for the interpreter. Return true if it was succeful and false if he fail.
    private fun tryGPU() : Boolean {

        return false
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
        return interpreter != null
    }

    // Main function that generate a random face
    fun generateFace() : Bitmap{

        val randomNoise = FloatArray(100)
        val rand = Random()

        for (x in 0..99){
            randomNoise[x] = rand.nextGaussian().toFloat()
        }

        val input : TensorBuffer = TensorBuffer.createFixedSize(intArrayOf(1,100), DataType.FLOAT32)
        input.loadArray(randomNoise)
        val output = TensorBuffer.createFixedSize(intArrayOf(1,modelResultSizeWidth,modelResultSizeHeight,3),DataType.FLOAT32)
        interpreter?.run(input.buffer, output.buffer)

        val resRaw = output.buffer

        return getOutputImage(resRaw)
    }

    //TODO see the colors
    private fun getOutputImage(output: ByteBuffer): Bitmap {
        output.rewind()

        val bitmap = Bitmap.createBitmap(modelResultSizeWidth, modelResultSizeHeight, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(modelResultSizeWidth * modelResultSizeHeight)
        for (i in 0 until modelResultSizeWidth * modelResultSizeHeight) {
            val a = 0xFF

            // In the result of our models, we have a float array with float between -1 and 1
            // and we want to get value between 0 and 255
            val r = ((output.float + 1) / 2) * 255.0f
            val g = ((output.float + 1) / 2) * 255.0f
            val b = ((output.float + 1) / 2) * 255.0f

            Log.d("DEBUG", "value : $r $g $b")

            //pixels[i] = a shl 24 or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
            pixels[i] = a shl 24 or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
        }
        bitmap.setPixels(pixels, 0, modelResultSizeWidth, 0, 0, modelResultSizeWidth, modelResultSizeHeight)
        return bitmap
    }

    private fun inputStreamToAByteBuffer(inputS : InputStream): ByteBuffer? {

        val buffer = ByteArrayOutputStream()
        var nRead: Int
        val data = ByteArray(1024)

        nRead = inputS.read(data, 0, data.size)
        while (nRead != -1) {
            buffer.write(data, 0, nRead)
            nRead = inputS.read(data, 0, data.size)
        }

        buffer.flush()
        val bytes = buffer.toByteArray()
        val byteBuffer = ByteBuffer.allocateDirect(bytes.size)
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.put(bytes)

        return byteBuffer
    }

    fun close() {
        interpreter!!.close()
    }
}