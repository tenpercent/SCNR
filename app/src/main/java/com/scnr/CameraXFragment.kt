package com.scnr

import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class CameraXFragment : BaseFragment() {
    override val layoutRID: Int
        get() = R.layout.retrieval_screen

    lateinit var cameraPreviewView: PreviewView
    lateinit var camera: Camera
    lateinit var preview: Preview
    lateinit var cameraSelector: CameraSelector
    lateinit var cameraSurfaceProvider: Preview.SurfaceProvider
    lateinit var imageAnalysis: ImageAnalysis
    lateinit var cameraExecutor: Executor
    val imageBuffer: ByteBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraPreviewView = view.findViewById<PreviewView>(R.id.view_finder).also {
            it.post { bindCameraView() }
        }
    }

    private fun bindCameraView() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            preview = Preview.Builder()
                    .build()
            cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
            imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(WIDTH, HEIGHT))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { image ->
                image.use { ip: ImageProxy ->
                    val rotationDegrees = ip.imageInfo.rotationDegrees
                    val timestamp = ip.imageInfo.timestamp
                    imageBuffer.rewind()
                    imageToByteBuffer(ip, imageBuffer.array())
                    Log.d(TAG, "image analysis rotation degrees: $rotationDegrees")
                    Log.d(TAG, "image analysis timestamp: $timestamp")
                    Log.d(TAG, "data size: ${imageBuffer.array().size}")
                }
            })
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            cameraSurfaceProvider = cameraPreviewView.createSurfaceProvider(camera.cameraInfo)
            preview.setSurfaceProvider(cameraSurfaceProvider)

        }, ContextCompat.getMainExecutor(requireContext()))
    }
    companion object {
        val TAG = "CameraXFragment"

        val WIDTH = 720
        val HEIGHT = 1280

        private fun imageToByteBuffer(image: ImageProxy, outputBuffer: ByteArray) {
            assert(image.format == ImageFormat.YUV_420_888)

            val imageCrop = image.cropRect
            val imagePlanes = image.planes
            val pixelCount = image.cropRect.width() * image.cropRect.height()

            imagePlanes.forEachIndexed { planeIndex, plane ->
                // How many values are read in input for each output value written
                // Only the Y plane has a value for every pixel, U and V have half the resolution i.e.
                //
                // Y Plane            U Plane    V Plane
                // ===============    =======    =======
                // Y Y Y Y Y Y Y Y    U U U U    V V V V
                // Y Y Y Y Y Y Y Y    U U U U    V V V V
                // Y Y Y Y Y Y Y Y    U U U U    V V V V
                // Y Y Y Y Y Y Y Y    U U U U    V V V V
                // Y Y Y Y Y Y Y Y
                // Y Y Y Y Y Y Y Y
                // Y Y Y Y Y Y Y Y
                val outputStride: Int

                // The index in the output buffer the next value will be written at
                // For Y it's zero, for U and V we start at the end of Y and interleave them i.e.
                //
                // First chunk        Second chunk
                // ===============    ===============
                // Y Y Y Y Y Y Y Y    V U V U V U V U
                // Y Y Y Y Y Y Y Y    V U V U V U V U
                // Y Y Y Y Y Y Y Y    V U V U V U V U
                // Y Y Y Y Y Y Y Y    V U V U V U V U
                // Y Y Y Y Y Y Y Y
                // Y Y Y Y Y Y Y Y
                // Y Y Y Y Y Y Y Y
                var outputOffset: Int

                when (planeIndex) {
                    0 -> {
                        outputStride = 1
                        outputOffset = 0
                    }
                    1 -> {
                        outputStride = 2
                        // For NV21 format, U is in odd-numbered indices
                        outputOffset = pixelCount + 1
                    }
                    2 -> {
                        outputStride = 2
                        // For NV21 format, V is in even-numbered indices
                        outputOffset = pixelCount
                    }
                    else -> {
                        // Image contains more than 3 planes, something strange is going on
                        return@forEachIndexed
                    }
                }

                val planeBuffer = plane.buffer
                val rowStride = plane.rowStride
                val pixelStride = plane.pixelStride

                // We have to divide the width and height by two if it's not the Y plane
                val planeCrop = if (planeIndex == 0) {
                    imageCrop
                } else {
                    Rect(
                        imageCrop.left / 2,
                        imageCrop.top / 2,
                        imageCrop.right / 2,
                        imageCrop.bottom / 2
                    )
                }

                val planeWidth = planeCrop.width()
                val planeHeight = planeCrop.height()

                // Intermediate buffer used to store the bytes of each row
                val rowBuffer = ByteArray(plane.rowStride)

                // Size of each row in bytes
                val rowLength = if (pixelStride == 1 && outputStride == 1) {
                    planeWidth
                } else {
                    // Take into account that the stride may include data from pixels other than this
                    // particular plane and row, and that could be between pixels and not after every
                    // pixel:
                    //
                    // |---- Pixel stride ----|                    Row ends here --> |
                    // | Pixel 1 | Other Data | Pixel 2 | Other Data | ... | Pixel N |
                    //
                    // We need to get (N-1) * (pixel stride bytes) per row + 1 byte for the last pixel
                    (planeWidth - 1) * pixelStride + 1
                }

                for (row in 0 until planeHeight) {
                    // Move buffer position to the beginning of this row
                    planeBuffer.position(
                        (row + planeCrop.top) * rowStride + planeCrop.left * pixelStride)

                    if (pixelStride == 1 && outputStride == 1) {
                        // When there is a single stride value for pixel and output, we can just copy
                        // the entire row in a single step
                        planeBuffer.get(outputBuffer, outputOffset, rowLength)
                        outputOffset += rowLength
                    } else {
                        // When either pixel or output have a stride > 1 we must copy pixel by pixel
                        planeBuffer.get(rowBuffer, 0, rowLength)
                        for (col in 0 until planeWidth) {
                            outputBuffer[outputOffset] = rowBuffer[col * pixelStride]
                            outputOffset += outputStride
                        }
                    }
                }
            }
        }
    }
}
