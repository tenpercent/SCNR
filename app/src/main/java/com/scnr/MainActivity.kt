package com.scnr

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.scnr.ui.CameraXFragment
import com.scnr.ui.TextFragment

class MainActivity : FragmentActivity() {

    val vm: OCRViewModel by lazy {
        ViewModelProvider(this).get(OCRViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
    }

    override fun onResume() {
        super.onResume()
        findViewById<ViewPager2>(R.id.pager).adapter = object: FragmentStateAdapter(this) {
            override fun getItemCount() = 2

            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> CameraXFragment(vm)
                else -> TextFragment(vm)
            }
        }
    }

    companion object {
        val TAG = "scnr.MainActivity"
    }
}
