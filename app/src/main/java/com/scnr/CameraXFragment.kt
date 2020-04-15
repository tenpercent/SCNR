package com.scnr

import android.os.Bundle
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

class CameraXFragment : BaseFragment() {
    override val layoutRID: Int
        get() = R.layout.retrieval_screen

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cameraPreviewView = view.findViewById<PreviewView>(R.id.view_finder)

        cameraPreviewView.post {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
            cameraProviderFuture.addListener(Runnable {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder()
                    .build()
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
                val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview)
                preview.setSurfaceProvider(cameraPreviewView.createSurfaceProvider(camera.cameraInfo))

            }, ContextCompat.getMainExecutor(requireContext()))
        }
    }

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        // TODO
    }
}
