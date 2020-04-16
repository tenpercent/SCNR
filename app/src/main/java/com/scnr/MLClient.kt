package com.scnr

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import androidx.annotation.WorkerThread
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.streams.toList


class MLClient(private val context: Context) {
    private val options = Interpreter.Options().also {
        it.setNumThreads(8)
    }

    private val modelFileBuffer = loadModelFile(context.assets)

    val interpreter = Interpreter(modelFileBuffer, options)

    val dict = loadDictionaryFile(context.assets)

    companion object {
        val MODEL_PATH = "bert/model.tflite"
        val DIC_PATH = "bert/vocab.txt"
        val TAG = "scnr.MLClient"

        @WorkerThread
        private fun loadModelFile(assetManager: AssetManager): MappedByteBuffer =
            assetManager.openFd(MODEL_PATH).use { fd ->
                FileInputStream(fd.fileDescriptor).use { ist ->
                    return ist.channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        fd.startOffset,
                        fd.declaredLength
                    )
                }
            }

        @WorkerThread
        private fun loadDictionaryFile(assetManager: AssetManager): Map<String, Int> =
            assetManager.open(DIC_PATH).use { fd ->
                BufferedReader(InputStreamReader(fd)).use { reader ->
                    return reader.lines().toList().mapIndexed { index, s -> Pair(index, s) }.associateBy({it.second}, {it.first})
                }
            }
    }
}
