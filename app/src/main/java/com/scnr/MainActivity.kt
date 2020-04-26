package com.scnr

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import java.io.File
import java.io.FileOutputStream

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        File("${cacheDir}/tessdata").apply {
            if (!exists()) mkdir()
        }

        listOf("eng", "osd").map { "tessdata/$it.traineddata" }.forEach {fname ->
            assets.openFd(fname).use {
                it.createInputStream().channel.transferTo(
                    it.startOffset,
                    it.length,
                    FileOutputStream("${cacheDir}/$fname").channel)
            }
        }



    }

    override fun onResume() {
        super.onResume()
        findViewById<ViewPager2>(R.id.pager).adapter = object: FragmentStateAdapter(this) {
            override fun getItemCount() = 2

            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> CameraXFragment()
                else -> QAFragment()
            }
        }

        val client = MLClient(this, false)
        Log.d(TAG, client.interpreter.inputTensorCount.toString())
        Log.d(TAG, client.dict.toString())
        client.sampleRun()
    }

    companion object {
        val TAG = "scnr.MainActivity"

    }
}
