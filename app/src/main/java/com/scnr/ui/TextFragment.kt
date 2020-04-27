package com.scnr.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.scnr.OCRViewModel
import com.scnr.R

class TextFragment(val vm: OCRViewModel): BaseFragment() {
    override val layoutRID: Int
        get() = R.layout.ocrd

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.registerTextObserver { s: String ->
            activity?.runOnUiThread {
                view.findViewById<TextView>(R.id.the_answer).text = s
            }
        }
    }
}