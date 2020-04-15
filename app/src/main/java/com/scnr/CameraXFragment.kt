package com.scnr

import android.os.Bundle
import android.view.View
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

class CameraXFragment : BaseFragment() {
    override val layoutRID: Int
        get() = R.layout.retrieval_screen

    lateinit var cameraPreviewView: PreviewView
    lateinit var camera: Camera
    lateinit var preview: Preview
    lateinit var cameraSelector: CameraSelector
    lateinit var cameraSurfaceProvider: Preview.SurfaceProvider

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            cameraSurfaceProvider = cameraPreviewView.createSurfaceProvider(camera.cameraInfo)
            preview.setSurfaceProvider(cameraSurfaceProvider)

        }, ContextCompat.getMainExecutor(requireContext()))
    }
}
