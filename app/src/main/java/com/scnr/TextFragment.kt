package com.scnr

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment

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