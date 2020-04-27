package com.scnr

import android.content.res.AssetManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import java.io.File
import java.io.FileOutputStream

class MainActivity : FragmentActivity() {

    val vm: OCRViewModel by lazy {
        ViewModelProvider(this).get(OCRViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        cacheTessData(assets, cacheDir)
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

        val client = MLClient(this, false)
        Log.d(TAG, client.interpreter.inputTensorCount.toString())
        Log.d(TAG, client.dict.toString())
        client.sampleRun()
    }

    companion object {
        val TAG = "scnr.MainActivity"
        fun cacheTessData(assets: AssetManager, cacheDir: File) {
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
    }
}
