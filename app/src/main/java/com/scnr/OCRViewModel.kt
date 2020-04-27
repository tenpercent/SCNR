package com.scnr

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class OCRViewModel(app: Application): AndroidViewModel(app) {
    val text = MutableLiveData("")
    fun registerTextObserver(f: (String) -> Unit) = text.observeForever(f)
}