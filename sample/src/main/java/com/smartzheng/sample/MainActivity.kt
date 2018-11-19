package com.smartzheng.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        slide_rab.setRangeList(listOf("1km", "2km", "3km", "4km", "5km"))
        slide_rab.setInitialIndex(2)
        slide_rab.setOnSlideListener(object : RangeSliderView.OnSlideListener {
            override fun onSlide(index: Int, s: String) {
                Toast.makeText(this@MainActivity, index.toString(), Toast.LENGTH_SHORT).show()
            }

        })
    }
}
