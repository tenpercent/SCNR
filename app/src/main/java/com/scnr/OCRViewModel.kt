package com.scnr

import android.app.Application
import android.content.res.AssetManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream

class OCRViewModel(app: Application): AndroidViewModel(app) {

    fun registerTextObserver(f: (String) -> Unit) = text.observeForever(f)

    fun analyze(imageBytes: ByteArray, width: Int, height: Int): String {
        mTess.setImage(imageBytes, width, height, 1, width)
        return mTess.utF8Text.also { text.postValue(it); mTess.clear() }
    }

    init {
        cacheTessData(app.assets, app.cacheDir)
    }

    private val mTess = TessBaseAPI().apply {
        init("${app.cacheDir}", "eng")
        pageSegMode = TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT_OSD
    }

    val text = MutableLiveData("")

    companion object {
        val TAG = "scnr.OCRViewModel"
        fun cacheTessData(assets: AssetManager, cacheDir: File) {
            // create cache directory if it doesn't exist
            File("${cacheDir}/tessdata").apply {
                if (!exists()) mkdir()
            }
            // copy trained data from assets to cache directory
            listOf("eng", "osd").map { "tessdata/$it.traineddata" }.forEach {fname ->
                assets.openFd(fname).use {
                    it.createInputStream().channel.transferTo(
                        it.startOffset,
                        it.length,
                        FileOutputStream("${cacheDir}/$fname").channel)
                }
            }
        }
    }
}