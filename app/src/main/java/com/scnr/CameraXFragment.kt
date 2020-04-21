package com.scnr

import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
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
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { image ->
                image.use {
                    val rotationDegrees = it.imageInfo.rotationDegrees
                    val timestamp = it.imageInfo.timestamp
                    Log.d(TAG, "image analysis rotation degrees: $rotationDegrees")
                    Log.d(TAG, "image analysis timestamp: $timestamp")
                }
            })
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            cameraSurfaceProvider = cameraPreviewView.createSurfaceProvider(camera.cameraInfo)
            preview.setSurfaceProvider(cameraSurfaceProvider)

        }, ContextCompat.getMainExecutor(requireContext()))
    }
    companion object {
        val TAG = "CameraXFragment"
    }
}
