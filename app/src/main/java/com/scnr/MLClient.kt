package com.scnr

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import androidx.annotation.WorkerThread
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.streams.toList


class MLClient(private val context: Context, useAcceleration: Boolean) {
    private val options = Interpreter.Options().apply {
        setNumThreads(8)
        if (useAcceleration) {
            addDelegate(NnApiDelegate())
        }
    }

    private val modelFileBuffer = loadModelFile(context.assets)

    val interpreter = Interpreter(modelFileBuffer, options)

    val dict = loadDictionaryFile(context.assets)

    fun sampleRun() {
        val MAX_SEQ_LEN = 384
        val inputIds =
            Array(1) { IntArray(MAX_SEQ_LEN) {101} }
        val inputMask =
            Array(1) { IntArray(MAX_SEQ_LEN) {101} }
        val segmentIds =
            Array(1) { IntArray(MAX_SEQ_LEN) {101} }
        val startLogits =
            Array(1) { FloatArray(MAX_SEQ_LEN) { (-1).toFloat() } }
        val endLogits =
            Array(1) { FloatArray(MAX_SEQ_LEN) { (-1).toFloat() } }

        val inputs = arrayOf<Any>(inputIds, inputMask, segmentIds)
        val output: MutableMap<Int, Any> = HashMap()
        output[0] = endLogits
        output[1] = startLogits

        Log.d(TAG, "Run inference...")
        interpreter.runForMultipleInputsOutputs(inputs, output)
        Log.d(TAG, "Inferred tensors: ${startLogits[0].joinToString { "$it " }} and ${endLogits[0].joinToString { "$it " }}")
    }

    companion object {
        val MODEL_PATH = "bert/model.tflite"
        val DIC_PATH = "bert/vocab.txt"
        val TAG = "scnr.MLClient"

        @WorkerThread
        private fun loadModelFile(assetManager: AssetManager): MappedByteBuffer =
            assetManager.openFd(MODEL_PATH).use { fd ->
                FileInputStream(fd.fileDescriptor).use { ins ->
                    return ins.channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        fd.startOffset,
                        fd.declaredLength
                    )
                }
            }

        @WorkerThread
        private fun loadDictionaryFile(assetManager: AssetManager): Map<String, Int> =
            assetManager.open(DIC_PATH).use { ins ->
                BufferedReader(InputStreamReader(ins)).use { reader ->
                    return reader.lines().toList().mapIndexed { index, s -> Pair(index, s) }.associateBy({it.second}, {it.first})
                }
            }
    }
}
