package com.scnr

import android.graphics.ImageFormat
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

object ImageUtils {
    internal fun imageToGreyByteBuffer(image: ImageProxy, outputBuffer: ByteBuffer): Int {
        assert(image.format == ImageFormat.YUV_420_888)
        outputBuffer.rewind()
        image.planes[0].buffer.get(outputBuffer.array(), 0, image.width * image.height)
        return image.width * image.height
    }
}