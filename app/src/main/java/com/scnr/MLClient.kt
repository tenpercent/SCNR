package com.scnr

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import androidx.annotation.WorkerThread
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


class MLClient(private val context: Context) {
    private val options = Interpreter.Options().also {
        it.setNumThreads(8)
    }

    private val modelFileBuffer by lazy {
        loadModelFile(context.assets).also {
            Log.d(TAG, "Model loaded")
        }
    }

    val interpreter by lazy {
        Interpreter(modelFileBuffer, options)
    }

    companion object {
        val MODEL_PATH = "bert/model.tflite"
        val TAG = "scnr.MLClient"

        @WorkerThread
        fun loadModelFile(assetManager: AssetManager): MappedByteBuffer {
            Log.d(TAG, "assets: ${assetManager.list("")!!.joinToString { it }}")
            assetManager.openFd(MODEL_PATH).use { fileDescriptor ->
                FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                    return inputStream.channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        fileDescriptor.startOffset,
                        fileDescriptor.declaredLength
                    )
                }
            }
        }
    }


}