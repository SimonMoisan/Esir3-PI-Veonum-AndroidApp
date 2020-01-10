package com.example.industrialproject

import android.net.Uri
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate;
import java.io.File
import java.lang.Exception
import kotlin.random.Random

class TensorModelManager {

    private var interpreter : Interpreter? = null
    private var gpuDelegate : GpuDelegate? = null

    fun loadModelFromPath(path : String){
        val modelFile = File(path)

        if(tryGPU()){
            val gpuOption = Interpreter.Options().addDelegate(gpuDelegate)
            interpreter = Interpreter(modelFile, gpuOption)
        }else{
            interpreter = Interpreter(modelFile)
        }
    }

    fun loadModelDefault(){
        val modelFile = File(Uri.parse("file:///android_asset/models/default_model.tflite").toString())

        if(tryGPU()){
            val gpuOption = Interpreter.Options().addDelegate(gpuDelegate)
            interpreter = Interpreter(modelFile, gpuOption)
        }else{
            interpreter = Interpreter(modelFile)
        }
    }

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

    fun isOperational() : Boolean{
        return interpreter == null
    }

    fun generateFace() {

        //interpreter?.run()
    }

    fun close() {
        interpreter?.close()
    }
}