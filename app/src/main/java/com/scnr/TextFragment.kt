package com.scnr

import android.os.Bundle
import android.view.View
import android.widget.TextView

class TextFragment(val vm: OCRViewModel): BaseFragment() {
    override val layoutRID: Int
        get() = R.layout.ocrd

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.registerTextObserver { s: String ->
            view.findViewById<TextView>(R.id.the_answer).text = s
        }
    }

    override fun onResume() {
        super.onResume()
    }
//
//    override fun onResume() {
//        super.onResume()
//    }
}