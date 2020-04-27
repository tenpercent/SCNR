package com.scnr

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.googlecode.tesseract.android.TessBaseAPI
import com.googlecode.tesseract.android.TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT
import com.googlecode.tesseract.android.TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT_OSD
import java.nio.ByteBuffer

class CameraXFragment(val vm: OCRViewModel) : BaseFragment() {
    override val layoutRID: Int
        get() = R.layout.retrieval_screen

    private lateinit var cameraPreviewView: PreviewView
    lateinit var camera: Camera
    lateinit var preview: Preview
    lateinit var cameraSelector: CameraSelector
    lateinit var cameraSurfaceProvider: Preview.SurfaceProvider
    lateinit var cameraProvider: ProcessCameraProvider
    lateinit var imageAnalysis: ImageAnalysis
    private var displayId: Int = -1
    // TODO: detect rotation properly
    private val imageBuffer:ByteBuffer = ByteBuffer.allocateDirect(1080 * 1080)
    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraXFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                imageAnalysis.targetRotation = view.display.rotation
            }
        } ?: Unit
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraPreviewView = view.findViewById<PreviewView>(R.id.view_finder).also {
            it.post {
                displayId = it.display.displayId
            }
        }
    }

    override fun onResume() {
        super.onResume()
        displayManager.registerDisplayListener(displayListener, null)
        bindCameraView()
    }

    override fun onPause() {
        super.onPause()
        cameraProvider.unbindAll()
        displayManager.unregisterDisplayListener(displayListener)
        imageAnalysis.clearAnalyzer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun bindCameraView() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()
            preview = Preview.Builder()
                    .build()
            cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
            imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(WIDTH, HEIGHT))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            val cameraExecutor = ContextCompat.getMainExecutor(requireContext())
            imageAnalysis.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { image ->
                Log.d(TAG, "image analyser rect: ${image.cropRect}")
                image.use { ip: ImageProxy ->
                    val startTime = System.currentTimeMillis()
                    try {
                        // copy greyscale image as bytes
                        ip.planes[0].buffer.get(imageBuffer.array(), 0, ip.width * ip.height)

                        val textFromImage = vm.analyze(imageBuffer.array(), ip.width, ip.height)

                        Log.d(TAG, "tesseract recognized: ${textFromImage}")
                    } catch (e: ArrayIndexOutOfBoundsException) {
                    }
                    Log.d(TAG, "milliseconds per frame: ${System.currentTimeMillis() - startTime}")
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
    }
}
