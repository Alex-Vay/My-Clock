package com.nr.myclock.games.memory

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nr.myclock.clock.activities.MainActivity
import com.nr.myclock.databinding.MemoryActivityBinding
import kotlin.random.Random


class MemoryGameActivity : AppCompatActivity() {
    private lateinit var binding : MemoryActivityBinding
    private var numCount = -1
    private var minNum = -1
    private var maxNum = -1
    private var sleepTime = -1L
    private lateinit var placesNum : List<TextView>
    private lateinit var showedNum : List<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MemoryActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        placesNum = listOf(binding.numPlace1, binding.numPlace2, binding.numPlace3,
            binding.numPlace4, binding.numPlace5, binding.numPlace6,
            binding.numPlace7, binding.numPlace8, binding.numPlace9,
            binding.numPlace10, binding.numPlace11, binding.numPlace12,
            binding.numPlace13, binding.numPlace14, binding.numPlace15).shuffled()
        val level = Random.nextInt(1, 4)
        if (level == 1) {
            numCount = 5
            minNum = 1
            maxNum = 10
            sleepTime = 7000L
        }
        else if (level == 2) {
            numCount = 9
            minNum = 1
            maxNum = 30
            sleepTime = 10000L
        }
        else {
            numCount = 15
            minNum = 30
            maxNum = 100
            sleepTime = 12000L
        }
        showedNum = (minNum..maxNum).shuffled()
        placeNumOnScreen()
        val z = showedNum.toIntArray()
        Handler().postDelayed({
            val finish = Intent(this, FinishActivity::class.java)
            startActivity(finish.putExtra("numCount", numCount).putExtra("showedNum", showedNum.toIntArray()))
        }, sleepTime)
    }

    private fun placeNumOnScreen() {
        for (i in 0 until  numCount) {
            val par = placesNum[i].layoutParams as ViewGroup.MarginLayoutParams
            par.leftMargin = Random.nextInt(-(i * 8 + 1), (i * 8 + 1))
            par.topMargin = Random.nextInt(-(i * 8 + 1), (i * 8 + 1))
            placesNum[i].layoutParams = par
            placesNum[i].text = showedNum[i].toString()
        }
    }
}