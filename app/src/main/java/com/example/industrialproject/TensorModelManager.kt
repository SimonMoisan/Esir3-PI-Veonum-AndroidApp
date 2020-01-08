package com.example.industrialproject

import org.tensorflow.lite.Interpreter

class TensorModelManager {

    private var interpreter : Interpreter? = null

    fun isOperational() : Boolean{
        return interpreter == null
    }

    fun close() {
        interpreter?.close()
    }
}